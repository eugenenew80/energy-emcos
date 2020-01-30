package energy.emcos.services.emcos;

import energy.emcos.services.http.HttpRequestServiceImpl;
import energy.emcos.model.entity.*;
import energy.emcos.model.repo.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

@Service
@RequiredArgsConstructor
public class ValueAtRequestService implements ValueRequestService {
    private final TemplateRegistry templateRegistry;
    private final ValueAtRepo valueAtRepo;
    private final PointAtRepo pointAtRepo;
    private final PointAtParamRepo pointAtParamRepo;
    private static final Logger logger = LoggerFactory.getLogger(ValueAtRequestService.class);
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HH:mm:'00000'");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public void load(ServerConfig config) throws Exception {
        logger.info("-------------------------------------------------------------");
        logger.info("ValueAtRequestService.load started");

        Integer countInGroup = 20;
        LocalDate nowDate = LocalDate.now(ZoneId.of(config.getTimeZone()));

        LocalDate defaultStartDate = nowDate.minusDays(config.getArcDepth().intValue());
        LocalDate defaultEndDate = nowDate.plusDays(1);

        logger.debug("default start date: {}", defaultStartDate);
        logger.debug("default end date: {}", defaultEndDate);

        List<PointConfig> allPoints = buildPoints(defaultStartDate, defaultEndDate);
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

        //Загружаем данные
        for (List<PointConfig> points: splitPoints){
            List<ValueAt> list;
            try {
                byte[] body = buildBody(config, points);
                if (body == null || body.length == 0) {
                    logger.info("Request body is empty, request terminated");
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
                logger.info("request successfully completed");

                if (list.size() > 0) {
                    logger.info("saving data in db...");
                    saveAtValues(list);
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
        logger.info("ValueAtRequestService.load completed");
        logger.info("-------------------------------------------------------------");
    }

    private List<PointConfig> buildPoints(LocalDate defaultStartDate, LocalDate defaultEndDate) {
        List<PointConfig> list = new ArrayList<>();

        List<PointAt> points = pointAtRepo.findAll();
        for (PointAt point : points){
            List<PointAtParam> pointParams = pointAtParamRepo.findAllByPointCode(point.getCode());
            for (PointAtParam paramParam : pointParams) {
                LocalDate startDate = paramParam.getLastMeteringDate() == null || paramParam.getLastMeteringDate().toLocalDate().isBefore(defaultStartDate)
                    ? defaultStartDate
                    : paramParam.getLastMeteringDate().toLocalDate().plusDays(1);

                LocalDateTime startTime = LocalDateTime.of(startDate, LocalTime.of(0, 0));
                LocalDateTime endTime = LocalDateTime.of(defaultEndDate, LocalTime.of(0, 0));

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
            .map( p-> buildNode(p))
            .filter(p -> StringUtils.isNotEmpty(p))
            .collect(Collectors.joining());
        logger.trace("points: " + strPoints);

        if (StringUtils.isEmpty(strPoints)) {
            logger.debug("List of points is empty, ValueAtService.buildBody stopped");
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

        logger.trace("body part 1 for request metering aData: " + body1);
        logger.trace("body part 2 for request metering aData: " + body2);

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

    private String buildNode(PointConfig point) {
        return ""
            + "<ROW PPOINT_CODE=\"" + point.getPointCode() + "\" "
            + "PML_ID=\"" + point.getParamCode() + "\" "
            + "PBT=\"" + point.getStartTime().format(timeFormatter) + "\" "
            + "PET=\"" + point.getEndTime().format(timeFormatter) + "\" />";
    }

    private List<ValueAt> parseAnswer(String answer) throws Exception {
        logger.info("parseAnswer started");
        logger.trace("answer: " + new String(Base64.decodeBase64(answer), "Cp1251"));

        logger.debug("parsing xml started");
        Document doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader( new String(Base64.decodeBase64(answer), "Cp1251") )));
        logger.debug("parsing xml completed");

        logger.debug("convert xml to list started");
        NodeList nodes =  doc.getDocumentElement().getParentNode()
            .getFirstChild()
            .getChildNodes();

        List<ValueAt> list = new ArrayList<>();
        for(int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeName() == "ROWDATA") {
                NodeList rowData = nodes.item(i).getChildNodes();
                for(int j = 0; j < rowData.getLength(); j++) {
                    if (rowData.item(j).getNodeName() == "ROW") {
                        logger.trace("row: " + (j+1));
                        ValueAt node = parseNode(rowData.item(j));
                        if (node!=null)
                            list.add(node);
                    }
                }
            }
        }
        logger.debug("convert xml to list completed");

        logger.info("parseAnswer completed, count of rows: " + list.size());
        return list;
    }

    private ValueAt parseNode(Node node) {
        NamedNodeMap attributes = node.getAttributes();

        String externalCode = attributes
            .getNamedItem("PPOINT_CODE")
            .getNodeValue() ;

        String sourceParamCode = attributes
            .getNamedItem("PML_ID")
            .getNodeValue() ;

        LocalDate date = null;
        String dateStr = attributes
            .getNamedItem("PBT")
            .getNodeValue();

        if (dateStr.length() > 8)
            return null;

        dateStr = dateStr.substring(0, 8);
        try {
            if (dateStr != null)
                date = LocalDate.parse(dateStr, dateFormatter);
        }
        catch (Exception e) {
            logger.error("parse date error :  " + e.getMessage());
            logger.error("dateStr = " + dateStr);
            logger.error("sourceParamCode = " + sourceParamCode);
            logger.error("externalCode = " + externalCode);
        }
        if (date==null)
            return null;

        Double val = null;
        String valStr = attributes
            .getNamedItem("PVAL")
            .getNodeValue();

        if (valStr!=null)
            val = Double.parseDouble(valStr);

        ValueAt value = new ValueAt();
        value.setPointCode(externalCode);
        value.setMeteringDate(date.atStartOfDay());
        value.setParamCode(sourceParamCode);
        value.setVal(val);

        return value;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void saveAtValues(List<ValueAt> values) {
        List<PointAtParam> pointParams = new ArrayList<>();
        for (ValueAt v : values) {
            Optional<ValueAt> valueCur = valueAtRepo.findOne(
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

            PointAtParamId ppId = new PointAtParamId(v.getPointCode(), v.getParamCode());
            PointAtParam pp = pointAtParamRepo
                .findById(ppId)
                .orElseGet(PointAtParam::new);

            pp.setPointCode(ppId.getPointCode());
            pp.setParamCode(ppId.getParamCode());
            pp.setLastLoadDate(LocalDateTime.now());
            if (pp.getLastMeteringDate() == null || pp.getLastMeteringDate().isBefore(v.getMeteringDate()))
                pp.setLastMeteringDate(v.getMeteringDate());
            pointParams.add(pp);
        }
        valueAtRepo.saveAll(values);
        pointAtParamRepo.saveAll(pointParams);
    }
}
