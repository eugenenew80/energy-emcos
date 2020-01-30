package energy.emcos.services.emcos;

import energy.emcos.services.http.HttpRequestServiceImpl;
import energy.emcos.model.entity.*;
import energy.emcos.model.repo.PointPtParamRepo;
import energy.emcos.model.repo.PointPtRepo;
import energy.emcos.model.repo.ValuePtRepo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.tomcat.util.codec.binary.Base64;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.apache.tomcat.util.codec.binary.Base64.decodeBase64;

@Service
@RequiredArgsConstructor
public class ValuePtRequestService {
    private final PointPtRepo pointPtRepo;
    private final ValuePtRepo valuePtRepo;
    private final PointPtParamRepo pointPtParamRepo;
    private final TemplateRegistry templateRegistry;
    private static final Logger logger = LoggerFactory.getLogger(ValuePtRequestService.class);
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HH:mm:'00000'");

    public void load(ServerConfig config) throws Exception {
        logger.info("-------------------------------------------------------------");
        logger.info("ValuePtRequestService.load started");

        Integer countInGroup = 20;
        LocalDateTime now = LocalDateTime.now(ZoneId.of(config.getTimeZone()));
        LocalDate nowDate = now.toLocalDate();
        LocalTime nowTime = LocalTime.of(now.getHour(), new Double(Math.floor(now.getMinute() / 15) * 15).intValue(), 0);

        LocalDateTime defaultStartTime = LocalDateTime.of(now.toLocalDate().minusDays(config.getArcDepth().intValue()), LocalTime.of(0,0));
        LocalDateTime defaultEndDateTime = LocalDateTime.of(nowDate, nowTime);

        logger.debug("default start time: {}", defaultStartTime);
        logger.debug("default end time: {}", defaultEndDateTime);

        List<PointConfig> allPoints = buildPoints(defaultStartTime, defaultEndDateTime);
        if (allPoints.isEmpty()) {
            logger.info("all data is up to date");
            return;
        }

        //Разбиваем список на группы
        List<List<PointConfig>> splitPoints = range(0, allPoints.size())
            .boxed()
            .collect(groupingBy(index -> index / countInGroup))
            .values()
            .stream()
            .map(indices -> indices.stream()
                .map(allPoints::get)
                .collect(toList()))
            .collect(toList());

        for (List<PointConfig> points: splitPoints) {
            List<ValuePt> list;
            try {
                logger.info("Send http request for metering media...");
                byte[] body = buildBody(config, points);
                if (body == null || body.length == 0) {
                    logger.info("Request body is empty, request stopped");
                    return;
                }

                byte[] byteAnswer = HttpRequestServiceImpl.builder()
                    .url(new URL(config.getUrl()))
                    .method("POST")
                    .body(body)
                    .build()
                    .doRequest();

                String answer = new String(byteAnswer, "UTF-8");
                int n1 = answer.indexOf("<AnswerData>");
                int n2 = answer.indexOf("</AnswerData>");
                if (n2 > n1)
                    answer = answer.substring(n1 + 12, n2);

                list = parseAnswer(answer);
                logger.info("request competed");

                if (list.size() > 0) {
                    logger.info("saving data in db...");
                    savePtValues(list);
                    logger.info("OK, {} records saved", list.size());
                }

                if (list.isEmpty())
                    logger.info("no data found");
            }
            catch (Exception e) {
                logger.error("request failed: " + e.toString());
                throw e;
            }
        }
        logger.info("ValuePtRequestService.load completed");
        logger.info("-------------------------------------------------------------");
    }

    private List<PointConfig> buildPoints(LocalDateTime defaultStartTime, LocalDateTime defaultEndTime) {
        List<PointConfig> list = new ArrayList<>();

        List<PointPt> points = pointPtRepo.findAll();
        for (PointPt point : points){
            List<PointPtParam> pointParams = pointPtParamRepo.findAllByPointCode(point.getCode());
            for (PointPtParam paramParam : pointParams) {
                LocalDateTime startTime = paramParam.getLastMeteringDate() == null || paramParam.getLastMeteringDate().isBefore(defaultStartTime)
                    ? defaultStartTime
                    : paramParam.getLastMeteringDate().plusMinutes(15);

                LocalDateTime endTime = defaultEndTime;

                PointConfig pc = new PointConfig();
                pc.setPointCode(point.getCode());
                pc.setParamCode(paramParam.getParamCode());
                pc.setStartTime(startTime);
                pc.setEndTime(endTime);
                if (startTime.isBefore(endTime))
                    list.add(pc);
            }
        }

        logger.trace("{}", list);
        return list;
    }

    private byte[] buildBody(ServerConfig config, List<PointConfig> points) {
    	logger.debug("buildBody started");

    	String strPoints = points.stream()
            .filter(p -> !p.getStartTime().isAfter(p.getEndTime()))
            .map( p-> buildPoint(p))
            .filter(p -> StringUtils.isNotEmpty(p))
            .collect(Collectors.joining());
        logger.trace("points: " + strPoints);

        if (StringUtils.isEmpty(strPoints)) {
        	logger.debug("List of points is empty, buildBody stopped");
            return new byte[0];
        }

        String aData = templateRegistry.getTemplate("EMCOS_REQML_DATA")
        	.replace("#points#", strPoints);
        logger.trace("media: " + aData);

        String property = templateRegistry.getTemplate("EMCOS_REQML_PROPERTY")
        	.replace("#user#", config.getUserName())
        	.replace("#isPacked#", "false")
        	.replace("#func#", "REQML")
        	.replace("#attType#", "1");
        logger.trace("property: " + property);

        String body1 = templateRegistry.getTemplate("EMCOS_REQML_BODY_1")
        	.replace("#property#", property);

        String body2 = templateRegistry.getTemplate("EMCOS_REQML_BODY_2")
        	.replace("#property#", property);

        logger.trace("body part 1 for request metering data: " + body1);
        logger.trace("body part 2 for request metering data: " + body2);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(body1.getBytes());
            baos.write(Base64.encodeBase64(aData.getBytes()));
            baos.write(body2.getBytes());
            baos.flush();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try { baos.close(); }
            catch (IOException e) { }
        }

        logger.debug("buildBody completed");
        return baos.toByteArray();
    }

    private String buildPoint(PointConfig point) {
        return ""
            + "<ROW PPOINT_CODE=\"" + point.getPointCode() + "\" "
            + "PML_ID=\"" + point.getParamCode() + "\" "
            + "PBT=\"" + point.getStartTime().format(timeFormatter) + "\" "
            + "PET=\"" + point.getEndTime().format(timeFormatter) + "\" />";
}

    private List<ValuePt> parseAnswer(String answer) throws Exception {
    	logger.info("parseAnswer started");
        logger.trace("answer: " + new String(decodeBase64(answer), "Cp1251"));

        logger.debug("parsing xml started");
        Document doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader( new String(decodeBase64(answer), "Cp1251") )));
        logger.debug("parsing xml completed");
        
        logger.debug("convert xml to list started");
        NodeList nodes =  doc.getDocumentElement().getParentNode()
            .getFirstChild()
            .getChildNodes();

        List<ValuePt> list = new ArrayList<>();
        for(int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeName() == "ROWDATA") {
                NodeList rowData = nodes.item(i).getChildNodes();
                for(int j = 0; j < rowData.getLength(); j++) {
                    if (rowData.item(j).getNodeName() == "ROW") {
                    	logger.trace("row: " + (j+1));
                        list.add(parseNode(rowData.item(j)));
                    }
                }
            }
        }
        logger.debug("convert xml to list completed");

        logger.info("parseAnswer completed, count of rows: " + list.size());
        return list;
    }

    private ValuePt parseNode(Node node) {
        NamedNodeMap attributes = node.getAttributes();

        String externalCode = attributes
            .getNamedItem("PPOINT_CODE")
            .getNodeValue() ;

        String sourceParamCode = attributes
            .getNamedItem("PML_ID")
            .getNodeValue() ;
        
        LocalDateTime time = null;
        String timeStr = attributes
            .getNamedItem("PBT")
            .getNodeValue() ;

        if (timeStr!=null) {
            if (timeStr.indexOf("T")<0) timeStr = timeStr+"T00:00:00000";
            time = LocalDateTime.parse(timeStr, timeFormatter);
        }

        Double val = null;
        String valStr = attributes
            .getNamedItem("PVAL")
            .getNodeValue();

        if (valStr!=null)
            val = Double.parseDouble(valStr);

        ValuePt value = new ValuePt();
        value.setPointCode(externalCode);
        value.setMeteringDate(time);
        value.setParamCode(sourceParamCode);
        value.setVal(val);
        
        return value;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void savePtValues(List<ValuePt> values) {
        List<PointPtParam> pointParams = new ArrayList<>();
        for (ValuePt v : values) {
            Optional<ValuePt> valueCur = valuePtRepo.findOne(
                v.getPointCode(),
                v.getParamCode(),
                v.getMeteringDate()
            );

            if (valueCur.isPresent()) {
                v.setId(valueCur.get().getId());
                v.setCreatedDate(valueCur.get().getCreatedDate());
                v.setLastUpdatedDate(LocalDateTime.now());
            }
            else
                v.setCreatedDate(LocalDateTime.now());

            PointPtParamId ppId = new PointPtParamId(v.getPointCode(), v.getParamCode());
            PointPtParam pp = pointPtParamRepo
                .findById(ppId)
                .orElseGet(PointPtParam::new);

            pp.setPointCode(ppId.getPointCode());
            pp.setParamCode(ppId.getParamCode());
            pp.setLastLoadDate(LocalDateTime.now());
            if (pp.getLastMeteringDate() == null || pp.getLastMeteringDate().isBefore(v.getMeteringDate()))
                pp.setLastMeteringDate(v.getMeteringDate());
            pointParams.add(pp);
        }
        valuePtRepo.saveAll(values);
        pointPtParamRepo.saveAll(pointParams);
    }
}
