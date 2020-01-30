package energy.emcos.helper;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import java.io.File;
import java.sql.Connection;

public class DataSetLoader {

    public DataSetLoader(String scheme, String file) {
        this.scheme = scheme;
        this.file = file;
    }

    public DataSetLoader deleteAll(Connection connection) throws Exception {
        IDataSet setupDataSet = getDataSet(dataSetPath + file);
        DatabaseConnection databaseConnection = new DatabaseConnection(connection, scheme);
        DatabaseOperation.DELETE_ALL.execute(databaseConnection, setupDataSet);
        databaseConnection.close();
        return this;
    }

    public DataSetLoader cleanAndInsert(Connection connection) throws Exception {
        IDataSet setupDataSet = getDataSet(dataSetPath + file);
        DatabaseConnection databaseConnection = new DatabaseConnection(connection, scheme);
        DatabaseOperation.CLEAN_INSERT.execute(databaseConnection, setupDataSet);
        databaseConnection.close();
        return this;
    }

    private IDataSet getDataSet(String name) throws Exception {
        FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
        builder.setColumnSensing(true);
        return builder.build(new File(name));
    }

    private final String scheme;
    private final String file;
    private final String dataSetPath = "src/test/resources/data/";
}
