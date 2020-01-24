package de.aaaaaaah.velcom.backend.storage.db;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.jooq.AlterIndexOnStep;
import org.jooq.AlterIndexStep;
import org.jooq.AlterSchemaStep;
import org.jooq.AlterSequenceStep;
import org.jooq.AlterTableStep;
import org.jooq.AlterViewStep;
import org.jooq.Attachable;
import org.jooq.Batch;
import org.jooq.BatchBindStep;
import org.jooq.BindContext;
import org.jooq.Block;
import org.jooq.Catalog;
import org.jooq.CommentOnIsStep;
import org.jooq.CommonTableExpression;
import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.ConnectionCallable;
import org.jooq.ConnectionRunnable;
import org.jooq.ContextTransactionalCallable;
import org.jooq.ContextTransactionalRunnable;
import org.jooq.CreateIndexStep;
import org.jooq.CreateSchemaFinalStep;
import org.jooq.CreateSequenceFlagsStep;
import org.jooq.CreateTableColumnStep;
import org.jooq.CreateTypeStep;
import org.jooq.CreateViewAsStep;
import org.jooq.Cursor;
import org.jooq.DDLExportConfiguration;
import org.jooq.DDLFlag;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.DeleteQuery;
import org.jooq.DeleteWhereStep;
import org.jooq.DropIndexOnStep;
import org.jooq.DropSchemaStep;
import org.jooq.DropSequenceFinalStep;
import org.jooq.DropTableStep;
import org.jooq.DropTypeStep;
import org.jooq.DropViewFinalStep;
import org.jooq.Explain;
import org.jooq.Field;
import org.jooq.GrantOnStep;
import org.jooq.Index;
import org.jooq.InsertQuery;
import org.jooq.InsertSetStep;
import org.jooq.InsertValuesStep1;
import org.jooq.InsertValuesStep10;
import org.jooq.InsertValuesStep11;
import org.jooq.InsertValuesStep12;
import org.jooq.InsertValuesStep13;
import org.jooq.InsertValuesStep14;
import org.jooq.InsertValuesStep15;
import org.jooq.InsertValuesStep16;
import org.jooq.InsertValuesStep17;
import org.jooq.InsertValuesStep18;
import org.jooq.InsertValuesStep19;
import org.jooq.InsertValuesStep2;
import org.jooq.InsertValuesStep20;
import org.jooq.InsertValuesStep21;
import org.jooq.InsertValuesStep22;
import org.jooq.InsertValuesStep3;
import org.jooq.InsertValuesStep4;
import org.jooq.InsertValuesStep5;
import org.jooq.InsertValuesStep6;
import org.jooq.InsertValuesStep7;
import org.jooq.InsertValuesStep8;
import org.jooq.InsertValuesStep9;
import org.jooq.InsertValuesStepN;
import org.jooq.Internal;
import org.jooq.LoaderOptionsStep;
import org.jooq.MergeKeyStep1;
import org.jooq.MergeKeyStep10;
import org.jooq.MergeKeyStep11;
import org.jooq.MergeKeyStep12;
import org.jooq.MergeKeyStep13;
import org.jooq.MergeKeyStep14;
import org.jooq.MergeKeyStep15;
import org.jooq.MergeKeyStep16;
import org.jooq.MergeKeyStep17;
import org.jooq.MergeKeyStep18;
import org.jooq.MergeKeyStep19;
import org.jooq.MergeKeyStep2;
import org.jooq.MergeKeyStep20;
import org.jooq.MergeKeyStep21;
import org.jooq.MergeKeyStep22;
import org.jooq.MergeKeyStep3;
import org.jooq.MergeKeyStep4;
import org.jooq.MergeKeyStep5;
import org.jooq.MergeKeyStep6;
import org.jooq.MergeKeyStep7;
import org.jooq.MergeKeyStep8;
import org.jooq.MergeKeyStep9;
import org.jooq.MergeKeyStepN;
import org.jooq.MergeUsingStep;
import org.jooq.Meta;
import org.jooq.Name;
import org.jooq.Param;
import org.jooq.Parser;
import org.jooq.PlainSQL;
import org.jooq.Privilege;
import org.jooq.Queries;
import org.jooq.Query;
import org.jooq.QueryPart;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record10;
import org.jooq.Record11;
import org.jooq.Record12;
import org.jooq.Record13;
import org.jooq.Record14;
import org.jooq.Record15;
import org.jooq.Record16;
import org.jooq.Record17;
import org.jooq.Record18;
import org.jooq.Record19;
import org.jooq.Record2;
import org.jooq.Record20;
import org.jooq.Record21;
import org.jooq.Record22;
import org.jooq.Record3;
import org.jooq.Record4;
import org.jooq.Record5;
import org.jooq.Record6;
import org.jooq.Record7;
import org.jooq.Record8;
import org.jooq.Record9;
import org.jooq.RenderContext;
import org.jooq.Result;
import org.jooq.ResultQuery;
import org.jooq.Results;
import org.jooq.RevokeOnStep;
import org.jooq.RowCountQuery;
import org.jooq.SQL;
import org.jooq.SQLDialect;
import org.jooq.Schema;
import org.jooq.Select;
import org.jooq.SelectField;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.SelectQuery;
import org.jooq.SelectSelectStep;
import org.jooq.SelectWhereStep;
import org.jooq.Sequence;
import org.jooq.Source;
import org.jooq.Statement;
import org.jooq.Support;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableLike;
import org.jooq.TableRecord;
import org.jooq.TransactionalCallable;
import org.jooq.TransactionalRunnable;
import org.jooq.TruncateIdentityStep;
import org.jooq.UDT;
import org.jooq.UDTRecord;
import org.jooq.UpdatableRecord;
import org.jooq.UpdateQuery;
import org.jooq.UpdateSetFirstStep;
import org.jooq.WithAsStep;
import org.jooq.WithAsStep1;
import org.jooq.WithAsStep10;
import org.jooq.WithAsStep11;
import org.jooq.WithAsStep12;
import org.jooq.WithAsStep13;
import org.jooq.WithAsStep14;
import org.jooq.WithAsStep15;
import org.jooq.WithAsStep16;
import org.jooq.WithAsStep17;
import org.jooq.WithAsStep18;
import org.jooq.WithAsStep19;
import org.jooq.WithAsStep2;
import org.jooq.WithAsStep20;
import org.jooq.WithAsStep21;
import org.jooq.WithAsStep22;
import org.jooq.WithAsStep3;
import org.jooq.WithAsStep4;
import org.jooq.WithAsStep5;
import org.jooq.WithAsStep6;
import org.jooq.WithAsStep7;
import org.jooq.WithAsStep8;
import org.jooq.WithAsStep9;
import org.jooq.WithStep;
import org.jooq.conf.Settings;
import org.jooq.exception.ConfigurationException;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.InvalidResultException;
import org.jooq.exception.NoDataFoundException;
import org.jooq.exception.TooManyRowsException;
import org.jooq.tools.jdbc.MockCallable;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockRunnable;
import org.jooq.util.xml.jaxb.InformationSchema;

public class TrackedDSLContext implements DSLContext {

	private DSLContext context;
	private AtomicBoolean closed = new AtomicBoolean();

	TrackedDSLContext(DSLContext original) {
		this.context = original;
		// TODO
	}

	@Override
	public void close() throws DataAccessException {
		context.close();
		closed.set(true);
	}

	@Override
	public Schema map(Schema schema) {
		return context.map(schema);
	}

	@Override
	public <R extends Record> Table<R> map(Table<R> table) {
		return context.map(table);
	}

	@Override
	public Parser parser() {
		return context.parser();
	}

	@Override
	public Connection parsingConnection() {
		return context.parsingConnection();
	}

	@Override
	public DataSource parsingDataSource() {
		return context.parsingDataSource();
	}

	@Override
	public Connection diagnosticsConnection() {
		return context.diagnosticsConnection();
	}

	@Override
	public DataSource diagnosticsDataSource() {
		return context.diagnosticsDataSource();
	}

	@Override
	public Meta meta() {
		return context.meta();
	}

	@Override
	public Meta meta(DatabaseMetaData databaseMetaData) {
		return context.meta(databaseMetaData);
	}

	@Override
	public Meta meta(Catalog... catalogs) {
		return context.meta(catalogs);
	}

	@Override
	public Meta meta(Schema... schemas) {
		return context.meta(schemas);
	}

	@Override
	public Meta meta(Table<?>... tables) {
		return context.meta(tables);
	}

	@Override
	public Meta meta(InformationSchema informationSchema) {
		return context.meta(informationSchema);
	}

	@Override
	@Internal
	public Meta meta(Source... sources) {
		return context.meta(sources);
	}

	@Override
	public InformationSchema informationSchema(Catalog catalog) {
		return context.informationSchema(catalog);
	}

	@Override
	public InformationSchema informationSchema(Catalog... catalogs) {
		return context.informationSchema(catalogs);
	}

	@Override
	public InformationSchema informationSchema(Schema schema) {
		return context.informationSchema(schema);
	}

	@Override
	public InformationSchema informationSchema(Schema... schemas) {
		return context.informationSchema(schemas);
	}

	@Override
	public InformationSchema informationSchema(Table<?> table) {
		return context.informationSchema(table);
	}

	@Override
	public InformationSchema informationSchema(Table<?>... tables) {
		return context.informationSchema(tables);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MYSQL, SQLDialect.POSTGRES,
		SQLDialect.SQLITE})
	public Explain explain(Query query) {
		return context.explain(query);
	}

	@Override
	public <T> T transactionResult(TransactionalCallable<T> transactionalCallable) {
		return context.transactionResult(transactionalCallable);
	}

	@Override
	public <T> T transactionResult(ContextTransactionalCallable<T> contextTransactionalCallable)
		throws ConfigurationException {
		return context.transactionResult(contextTransactionalCallable);
	}

	@Override
	public void transaction(TransactionalRunnable transactionalRunnable) {
		context.transaction(transactionalRunnable);
	}

	@Override
	public void transaction(ContextTransactionalRunnable contextTransactionalRunnable)
		throws ConfigurationException {
		context.transaction(contextTransactionalRunnable);
	}

	@Override
	public <T> CompletionStage<T> transactionResultAsync(
		TransactionalCallable<T> transactionalCallable) throws ConfigurationException {
		return context.transactionResultAsync(transactionalCallable);
	}

	@Override
	public CompletionStage<Void> transactionAsync(
		TransactionalRunnable transactionalRunnable) throws ConfigurationException {
		return context.transactionAsync(transactionalRunnable);
	}

	@Override
	public <T> CompletionStage<T> transactionResultAsync(
		Executor executor, TransactionalCallable<T> transactionalCallable)
		throws ConfigurationException {
		return context.transactionResultAsync(executor, transactionalCallable);
	}

	@Override
	public CompletionStage<Void> transactionAsync(
		Executor executor, TransactionalRunnable transactionalRunnable)
		throws ConfigurationException {
		return context.transactionAsync(executor, transactionalRunnable);
	}

	@Override
	public <T> T connectionResult(ConnectionCallable<T> connectionCallable) {
		return context.connectionResult(connectionCallable);
	}

	@Override
	public void connection(ConnectionRunnable connectionRunnable) {
		context.connection(connectionRunnable);
	}

	@Override
	public <T> T mockResult(MockDataProvider mockDataProvider,
		MockCallable<T> mockCallable) {
		return context.mockResult(mockDataProvider, mockCallable);
	}

	@Override
	public void mock(MockDataProvider mockDataProvider,
		MockRunnable mockRunnable) {
		context.mock(mockDataProvider, mockRunnable);
	}

	@Override
	@Internal
	@Deprecated
	public RenderContext renderContext() {
		return context.renderContext();
	}

	@Override
	public String render(QueryPart queryPart) {
		return context.render(queryPart);
	}

	@Override
	public String renderNamedParams(QueryPart queryPart) {
		return context.renderNamedParams(queryPart);
	}

	@Override
	public String renderNamedOrInlinedParams(QueryPart queryPart) {
		return context.renderNamedOrInlinedParams(queryPart);
	}

	@Override
	public String renderInlined(QueryPart queryPart) {
		return context.renderInlined(queryPart);
	}

	@Override
	public List<Object> extractBindValues(QueryPart queryPart) {
		return context.extractBindValues(queryPart);
	}

	@Override
	public Map<String, Param<?>> extractParams(QueryPart queryPart) {
		return context.extractParams(queryPart);
	}

	@Override
	public Param<?> extractParam(QueryPart queryPart, String s) {
		return context.extractParam(queryPart, s);
	}

	@Override
	@Internal
	@Deprecated
	public BindContext bindContext(PreparedStatement preparedStatement) {
		return context.bindContext(preparedStatement);
	}

	@Override
	@Deprecated
	public int bind(QueryPart queryPart, PreparedStatement preparedStatement) {
		return context.bind(queryPart, preparedStatement);
	}

	@Override
	public void attach(Attachable... attachables) {
		context.attach(attachables);
	}

	@Override
	public void attach(Collection<? extends Attachable> collection) {
		context.attach(collection);
	}

	@Override
	@Support
	public <R extends Record> LoaderOptionsStep<R> loadInto(Table<R> table) {
		return context.loadInto(table);
	}

	@Override
	@Support
	public Queries queries(Query... queries) {
		return context.queries(queries);
	}

	@Override
	@Support
	public Queries queries(Collection<? extends Query> collection) {
		return context.queries(collection);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.MARIADB, SQLDialect.MYSQL,
		SQLDialect.POSTGRES})
	public Block begin(Statement... statements) {
		return context.begin(statements);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.MARIADB, SQLDialect.MYSQL,
		SQLDialect.POSTGRES})
	public Block begin(Collection<? extends Statement> collection) {
		return context.begin(collection);
	}

	@Override
	@PlainSQL
	@Support
	public RowCountQuery query(SQL sql) {
		return context.query(sql);
	}

	@Override
	@PlainSQL
	@Support
	public RowCountQuery query(String s) {
		return context.query(s);
	}

	@Override
	@PlainSQL
	@Support
	public RowCountQuery query(String s, Object... objects) {
		return context.query(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public RowCountQuery query(String s, QueryPart... queryParts) {
		return context.query(s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public Result<Record> fetch(SQL sql) throws DataAccessException {
		return context.fetch(sql);
	}

	@Override
	@PlainSQL
	@Support
	public Result<Record> fetch(String s) throws DataAccessException {
		return context.fetch(s);
	}

	@Override
	@PlainSQL
	@Support
	public Result<Record> fetch(String s, Object... objects) throws DataAccessException {
		return context.fetch(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public Result<Record> fetch(String s, QueryPart... queryParts) throws DataAccessException {
		return context.fetch(s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public Cursor<Record> fetchLazy(SQL sql) throws DataAccessException {
		return context.fetchLazy(sql);
	}

	@Override
	@PlainSQL
	@Support
	public Cursor<Record> fetchLazy(String s) throws DataAccessException {
		return context.fetchLazy(s);
	}

	@Override
	@PlainSQL
	@Support
	public Cursor<Record> fetchLazy(String s, Object... objects) throws DataAccessException {
		return context.fetchLazy(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public Cursor<Record> fetchLazy(String s, QueryPart... queryParts) throws DataAccessException {
		return context.fetchLazy(s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		SQL sql) {
		return context.fetchAsync(sql);
	}

	@Override
	@PlainSQL
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		String s) {
		return context.fetchAsync(s);
	}

	@Override
	@PlainSQL
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		String s, Object... objects) {
		return context.fetchAsync(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		String s, QueryPart... queryParts) {
		return context.fetchAsync(s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		Executor executor, SQL sql) {
		return context.fetchAsync(executor, sql);
	}

	@Override
	@PlainSQL
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		Executor executor, String s) {
		return context.fetchAsync(executor, s);
	}

	@Override
	@PlainSQL
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		Executor executor, String s, Object... objects) {
		return context.fetchAsync(executor, s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		Executor executor, String s, QueryPart... queryParts) {
		return context.fetchAsync(executor, s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public Stream<Record> fetchStream(SQL sql) throws DataAccessException {
		return context.fetchStream(sql);
	}

	@Override
	@PlainSQL
	@Support
	public Stream<Record> fetchStream(String s) throws DataAccessException {
		return context.fetchStream(s);
	}

	@Override
	@PlainSQL
	@Support
	public Stream<Record> fetchStream(String s, Object... objects) throws DataAccessException {
		return context.fetchStream(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public Stream<Record> fetchStream(String s,
		QueryPart... queryParts) throws DataAccessException {
		return context.fetchStream(s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public Results fetchMany(SQL sql) throws DataAccessException {
		return context.fetchMany(sql);
	}

	@Override
	@PlainSQL
	@Support
	public Results fetchMany(String s) throws DataAccessException {
		return context.fetchMany(s);
	}

	@Override
	@PlainSQL
	@Support
	public Results fetchMany(String s, Object... objects) throws DataAccessException {
		return context.fetchMany(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public Results fetchMany(String s, QueryPart... queryParts) throws DataAccessException {
		return context.fetchMany(s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public Record fetchOne(SQL sql) throws DataAccessException, TooManyRowsException {
		return context.fetchOne(sql);
	}

	@Override
	@PlainSQL
	@Support
	public Record fetchOne(String s) throws DataAccessException, TooManyRowsException {
		return context.fetchOne(s);
	}

	@Override
	@PlainSQL
	@Support
	public Record fetchOne(String s, Object... objects)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOne(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public Record fetchOne(String s, QueryPart... queryParts)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOne(s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public Record fetchSingle(SQL sql)
		throws DataAccessException, NoDataFoundException, TooManyRowsException {
		return context.fetchSingle(sql);
	}

	@Override
	@PlainSQL
	@Support
	public Record fetchSingle(String s)
		throws DataAccessException, NoDataFoundException, TooManyRowsException {
		return context.fetchSingle(s);
	}

	@Override
	@PlainSQL
	@Support
	public Record fetchSingle(String s, Object... objects)
		throws DataAccessException, NoDataFoundException, TooManyRowsException {
		return context.fetchSingle(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public Record fetchSingle(String s, QueryPart... queryParts)
		throws DataAccessException, NoDataFoundException, TooManyRowsException {
		return context.fetchSingle(s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public Optional<Record> fetchOptional(SQL sql)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOptional(sql);
	}

	@Override
	@PlainSQL
	@Support
	public Optional<Record> fetchOptional(String s)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOptional(s);
	}

	@Override
	@PlainSQL
	@Support
	public Optional<Record> fetchOptional(String s, Object... objects)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOptional(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public Optional<Record> fetchOptional(String s, QueryPart... queryParts)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOptional(s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public Object fetchValue(SQL sql)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchValue(sql);
	}

	@Override
	@PlainSQL
	@Support
	public Object fetchValue(String s)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchValue(s);
	}

	@Override
	@PlainSQL
	@Support
	public Object fetchValue(String s, Object... objects)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchValue(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public Object fetchValue(String s, QueryPart... queryParts)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchValue(s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public Optional<?> fetchOptionalValue(SQL sql)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchOptionalValue(sql);
	}

	@Override
	@PlainSQL
	@Support
	public Optional<?> fetchOptionalValue(String s)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchOptionalValue(s);
	}

	@Override
	@PlainSQL
	@Support
	public Optional<?> fetchOptionalValue(String s, Object... objects)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchOptionalValue(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public Optional<?> fetchOptionalValue(String s, QueryPart... queryParts)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchOptionalValue(s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public List<?> fetchValues(SQL sql) throws DataAccessException, InvalidResultException {
		return context.fetchValues(sql);
	}

	@Override
	@PlainSQL
	@Support
	public List<?> fetchValues(String s) throws DataAccessException, InvalidResultException {
		return context.fetchValues(s);
	}

	@Override
	@PlainSQL
	@Support
	public List<?> fetchValues(String s, Object... objects)
		throws DataAccessException, InvalidResultException {
		return context.fetchValues(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public List<?> fetchValues(String s, QueryPart... queryParts)
		throws DataAccessException, InvalidResultException {
		return context.fetchValues(s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public int execute(SQL sql) throws DataAccessException {
		return context.execute(sql);
	}

	@Override
	@PlainSQL
	@Support
	public int execute(String s) throws DataAccessException {
		return context.execute(s);
	}

	@Override
	@PlainSQL
	@Support
	public int execute(String s, Object... objects) throws DataAccessException {
		return context.execute(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public int execute(String s, QueryPart... queryParts) throws DataAccessException {
		return context.execute(s, queryParts);
	}

	@Override
	@PlainSQL
	@Support
	public ResultQuery<Record> resultQuery(SQL sql) {
		return context.resultQuery(sql);
	}

	@Override
	@PlainSQL
	@Support
	public ResultQuery<Record> resultQuery(String s) {
		return context.resultQuery(s);
	}

	@Override
	@PlainSQL
	@Support
	public ResultQuery<Record> resultQuery(String s, Object... objects) {
		return context.resultQuery(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public ResultQuery<Record> resultQuery(String s, QueryPart... queryParts) {
		return context.resultQuery(s, queryParts);
	}

	@Override
	@Support
	public Result<Record> fetch(ResultSet resultSet) throws DataAccessException {
		return context.fetch(resultSet);
	}

	@Override
	@Support
	public Result<Record> fetch(ResultSet resultSet,
		Field<?>... fields) throws DataAccessException {
		return context.fetch(resultSet, fields);
	}

	@Override
	@Support
	public Result<Record> fetch(ResultSet resultSet,
		DataType<?>... dataTypes) throws DataAccessException {
		return context.fetch(resultSet, dataTypes);
	}

	@Override
	@Support
	public Result<Record> fetch(ResultSet resultSet,
		Class<?>... classes) throws DataAccessException {
		return context.fetch(resultSet, classes);
	}

	@Override
	@Support
	public Record fetchOne(ResultSet resultSet) throws DataAccessException, TooManyRowsException {
		return context.fetchOne(resultSet);
	}

	@Override
	@Support
	public Record fetchOne(ResultSet resultSet, Field<?>... fields)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOne(resultSet, fields);
	}

	@Override
	@Support
	public Record fetchOne(ResultSet resultSet, DataType<?>... dataTypes)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOne(resultSet, dataTypes);
	}

	@Override
	@Support
	public Record fetchOne(ResultSet resultSet, Class<?>... classes)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOne(resultSet, classes);
	}

	@Override
	@Support
	public Record fetchSingle(ResultSet resultSet)
		throws DataAccessException, TooManyRowsException {
		return context.fetchSingle(resultSet);
	}

	@Override
	@Support
	public Record fetchSingle(ResultSet resultSet, Field<?>... fields)
		throws DataAccessException, NoDataFoundException, TooManyRowsException {
		return context.fetchSingle(resultSet, fields);
	}

	@Override
	@Support
	public Record fetchSingle(ResultSet resultSet, DataType<?>... dataTypes)
		throws DataAccessException, NoDataFoundException, TooManyRowsException {
		return context.fetchSingle(resultSet, dataTypes);
	}

	@Override
	@Support
	public Record fetchSingle(ResultSet resultSet, Class<?>... classes)
		throws DataAccessException, NoDataFoundException, TooManyRowsException {
		return context.fetchSingle(resultSet, classes);
	}

	@Override
	@Support
	public Optional<Record> fetchOptional(ResultSet resultSet)
		throws DataAccessException, NoDataFoundException, TooManyRowsException {
		return context.fetchOptional(resultSet);
	}

	@Override
	@Support
	public Optional<Record> fetchOptional(ResultSet resultSet,
		Field<?>... fields) throws DataAccessException, TooManyRowsException {
		return context.fetchOptional(resultSet, fields);
	}

	@Override
	@Support
	public Optional<Record> fetchOptional(ResultSet resultSet,
		DataType<?>... dataTypes) throws DataAccessException, TooManyRowsException {
		return context.fetchOptional(resultSet, dataTypes);
	}

	@Override
	@Support
	public Optional<Record> fetchOptional(ResultSet resultSet,
		Class<?>... classes) throws DataAccessException, TooManyRowsException {
		return context.fetchOptional(resultSet, classes);
	}

	@Override
	@Support
	public Object fetchValue(ResultSet resultSet)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchValue(resultSet);
	}

	@Override
	@Support
	public <T> T fetchValue(ResultSet resultSet, Field<T> field)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchValue(resultSet, field);
	}

	@Override
	@Support
	public <T> T fetchValue(ResultSet resultSet, DataType<T> dataType)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchValue(resultSet, dataType);
	}

	@Override
	@Support
	public <T> T fetchValue(ResultSet resultSet, Class<T> aClass)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchValue(resultSet, aClass);
	}

	@Override
	@Support
	public Optional<?> fetchOptionalValue(ResultSet resultSet)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchOptionalValue(resultSet);
	}

	@Override
	@Support
	public <T> Optional<T> fetchOptionalValue(ResultSet resultSet,
		Field<T> field) throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchOptionalValue(resultSet, field);
	}

	@Override
	@Support
	public <T> Optional<T> fetchOptionalValue(ResultSet resultSet,
		DataType<T> dataType)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchOptionalValue(resultSet, dataType);
	}

	@Override
	@Support
	public <T> Optional<T> fetchOptionalValue(ResultSet resultSet,
		Class<T> aClass) throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchOptionalValue(resultSet, aClass);
	}

	@Override
	@Support
	public List<?> fetchValues(ResultSet resultSet)
		throws DataAccessException, InvalidResultException {
		return context.fetchValues(resultSet);
	}

	@Override
	@Support
	public <T> List<T> fetchValues(ResultSet resultSet, Field<T> field)
		throws DataAccessException, InvalidResultException {
		return context.fetchValues(resultSet, field);
	}

	@Override
	@Support
	public <T> List<T> fetchValues(ResultSet resultSet, DataType<T> dataType)
		throws DataAccessException, InvalidResultException {
		return context.fetchValues(resultSet, dataType);
	}

	@Override
	@Support
	public <T> List<T> fetchValues(ResultSet resultSet, Class<T> aClass)
		throws DataAccessException, InvalidResultException {
		return context.fetchValues(resultSet, aClass);
	}

	@Override
	@Support
	public Cursor<Record> fetchLazy(ResultSet resultSet) throws DataAccessException {
		return context.fetchLazy(resultSet);
	}

	@Override
	@Support
	public Cursor<Record> fetchLazy(ResultSet resultSet,
		Field<?>... fields) throws DataAccessException {
		return context.fetchLazy(resultSet, fields);
	}

	@Override
	@Support
	public Cursor<Record> fetchLazy(ResultSet resultSet,
		DataType<?>... dataTypes) throws DataAccessException {
		return context.fetchLazy(resultSet, dataTypes);
	}

	@Override
	@Support
	public Cursor<Record> fetchLazy(ResultSet resultSet,
		Class<?>... classes) throws DataAccessException {
		return context.fetchLazy(resultSet, classes);
	}

	@Override
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		ResultSet resultSet) {
		return context.fetchAsync(resultSet);
	}

	@Override
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		ResultSet resultSet, Field<?>... fields) {
		return context.fetchAsync(resultSet, fields);
	}

	@Override
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		ResultSet resultSet, DataType<?>... dataTypes) {
		return context.fetchAsync(resultSet, dataTypes);
	}

	@Override
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		ResultSet resultSet, Class<?>... classes) {
		return context.fetchAsync(resultSet, classes);
	}

	@Override
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		Executor executor, ResultSet resultSet) {
		return context.fetchAsync(executor, resultSet);
	}

	@Override
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		Executor executor, ResultSet resultSet,
		Field<?>... fields) {
		return context.fetchAsync(executor, resultSet, fields);
	}

	@Override
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		Executor executor, ResultSet resultSet,
		DataType<?>... dataTypes) {
		return context.fetchAsync(executor, resultSet, dataTypes);
	}

	@Override
	@Support
	public CompletionStage<Result<Record>> fetchAsync(
		Executor executor, ResultSet resultSet,
		Class<?>... classes) {
		return context.fetchAsync(executor, resultSet, classes);
	}

	@Override
	@Support
	public Stream<Record> fetchStream(ResultSet resultSet) throws DataAccessException {
		return context.fetchStream(resultSet);
	}

	@Override
	@Support
	public Stream<Record> fetchStream(ResultSet resultSet,
		Field<?>... fields) throws DataAccessException {
		return context.fetchStream(resultSet, fields);
	}

	@Override
	@Support
	public Stream<Record> fetchStream(ResultSet resultSet,
		DataType<?>... dataTypes) throws DataAccessException {
		return context.fetchStream(resultSet, dataTypes);
	}

	@Override
	@Support
	public Stream<Record> fetchStream(ResultSet resultSet,
		Class<?>... classes) throws DataAccessException {
		return context.fetchStream(resultSet, classes);
	}

	@Override
	@Support
	public Result<Record> fetchFromTXT(String s) throws DataAccessException {
		return context.fetchFromTXT(s);
	}

	@Override
	@Support
	public Result<Record> fetchFromTXT(String s, String s1) throws DataAccessException {
		return context.fetchFromTXT(s, s1);
	}

	@Override
	@Support
	public Result<Record> fetchFromHTML(String s) throws DataAccessException {
		return context.fetchFromHTML(s);
	}

	@Override
	@Support
	public Result<Record> fetchFromCSV(String s) throws DataAccessException {
		return context.fetchFromCSV(s);
	}

	@Override
	@Support
	public Result<Record> fetchFromCSV(String s, char c) throws DataAccessException {
		return context.fetchFromCSV(s, c);
	}

	@Override
	@Support
	public Result<Record> fetchFromCSV(String s, boolean b) throws DataAccessException {
		return context.fetchFromCSV(s, b);
	}

	@Override
	@Support
	public Result<Record> fetchFromCSV(String s, boolean b, char c) throws DataAccessException {
		return context.fetchFromCSV(s, b, c);
	}

	@Override
	@Support
	public Result<Record> fetchFromJSON(String s) {
		return context.fetchFromJSON(s);
	}

	@Override
	@Support
	public Result<Record> fetchFromXML(String s) {
		return context.fetchFromXML(s);
	}

	@Override
	public Result<Record> fetchFromStringData(String[]... strings) {
		return context.fetchFromStringData(strings);
	}

	@Override
	public Result<Record> fetchFromStringData(List<String[]> list) {
		return context.fetchFromStringData(list);
	}

	@Override
	public Result<Record> fetchFromStringData(List<String[]> list,
		boolean b) {
		return context.fetchFromStringData(list, b);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep with(String s) {
		return context.with(s);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep with(String s, String... strings) {
		return context.with(s, strings);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep with(Name name) {
		return context.with(name);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep with(Name name, Name... names) {
		return context.with(name, names);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep with(String s,
		Function<? super Field<?>, ? extends String> function) {
		return context.with(s, function);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep with(String s,
		BiFunction<? super Field<?>, ? super Integer, ? extends String> biFunction) {
		return context.with(s, biFunction);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep1 with(String s, String s1) {
		return context.with(s, s1);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep2 with(String s, String s1, String s2) {
		return context.with(s, s1, s2);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep3 with(String s, String s1, String s2, String s3) {
		return context.with(s, s1, s2, s3);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep4 with(String s, String s1, String s2, String s3, String s4) {
		return context.with(s, s1, s2, s3, s4);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep5 with(String s, String s1, String s2, String s3, String s4,
		String s5) {
		return context.with(s, s1, s2, s3, s4, s5);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep6 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6) {
		return context.with(s, s1, s2, s3, s4, s5, s6);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep7 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep8 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep9 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep10 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9, String s10) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep11 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9, String s10, String s11) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep12 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9, String s10, String s11,
		String s12) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep13 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9, String s10, String s11,
		String s12, String s13) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep14 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9, String s10, String s11,
		String s12, String s13, String s14) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep15 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9, String s10, String s11,
		String s12, String s13, String s14, String s15) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep16 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9, String s10, String s11,
		String s12, String s13, String s14, String s15, String s16) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15,
			s16);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep17 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9, String s10, String s11,
		String s12, String s13, String s14, String s15, String s16, String s17) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15,
			s16, s17);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep18 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9, String s10, String s11,
		String s12, String s13, String s14, String s15, String s16, String s17, String s18) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15,
			s16, s17,
			s18);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep19 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9, String s10, String s11,
		String s12, String s13, String s14, String s15, String s16, String s17, String s18,
		String s19) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15,
			s16, s17,
			s18, s19);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep20 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9, String s10, String s11,
		String s12, String s13, String s14, String s15, String s16, String s17, String s18,
		String s19, String s20) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15,
			s16, s17,
			s18, s19, s20);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep21 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9, String s10, String s11,
		String s12, String s13, String s14, String s15, String s16, String s17, String s18,
		String s19, String s20, String s21) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15,
			s16, s17,
			s18, s19, s20, s21);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep22 with(String s, String s1, String s2, String s3, String s4,
		String s5, String s6, String s7, String s8, String s9, String s10, String s11,
		String s12, String s13, String s14, String s15, String s16, String s17, String s18,
		String s19, String s20, String s21, String s22) {
		return context.with(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15,
			s16, s17,
			s18, s19, s20, s21, s22);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep1 with(Name name, Name name1) {
		return context.with(name, name1);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep2 with(Name name, Name name1, Name name2) {
		return context.with(name, name1, name2);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep3 with(Name name, Name name1, Name name2,
		Name name3) {
		return context.with(name, name1, name2, name3);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep4 with(Name name, Name name1, Name name2,
		Name name3, Name name4) {
		return context.with(name, name1, name2, name3, name4);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep5 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5) {
		return context.with(name, name1, name2, name3, name4, name5);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep6 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6) {
		return context.with(name, name1, name2, name3, name4, name5, name6);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep7 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep8 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep9 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep10 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9, Name name10) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9,
			name10);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep11 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9, Name name10,
		Name name11) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9,
			name10,
			name11);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep12 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9, Name name10,
		Name name11, Name name12) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9,
			name10,
			name11, name12);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep13 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9, Name name10,
		Name name11, Name name12, Name name13) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9,
			name10,
			name11, name12, name13);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep14 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9, Name name10,
		Name name11, Name name12, Name name13, Name name14) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9,
			name10,
			name11, name12, name13, name14);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep15 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9, Name name10,
		Name name11, Name name12, Name name13, Name name14,
		Name name15) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9,
			name10,
			name11, name12, name13, name14, name15);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep16 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9, Name name10,
		Name name11, Name name12, Name name13, Name name14,
		Name name15, Name name16) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9,
			name10,
			name11, name12, name13, name14, name15, name16);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep17 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9, Name name10,
		Name name11, Name name12, Name name13, Name name14,
		Name name15, Name name16, Name name17) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9,
			name10,
			name11, name12, name13, name14, name15, name16, name17);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep18 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9, Name name10,
		Name name11, Name name12, Name name13, Name name14,
		Name name15, Name name16, Name name17, Name name18) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9,
			name10,
			name11, name12, name13, name14, name15, name16, name17, name18);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep19 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9, Name name10,
		Name name11, Name name12, Name name13, Name name14,
		Name name15, Name name16, Name name17, Name name18,
		Name name19) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9,
			name10,
			name11, name12, name13, name14, name15, name16, name17, name18, name19);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep20 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9, Name name10,
		Name name11, Name name12, Name name13, Name name14,
		Name name15, Name name16, Name name17, Name name18,
		Name name19, Name name20) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9,
			name10,
			name11, name12, name13, name14, name15, name16, name17, name18, name19, name20);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep21 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9, Name name10,
		Name name11, Name name12, Name name13, Name name14,
		Name name15, Name name16, Name name17, Name name18,
		Name name19, Name name20, Name name21) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9,
			name10,
			name11, name12, name13, name14, name15, name16, name17, name18, name19, name20, name21);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep22 with(Name name, Name name1, Name name2,
		Name name3, Name name4, Name name5, Name name6,
		Name name7, Name name8, Name name9, Name name10,
		Name name11, Name name12, Name name13, Name name14,
		Name name15, Name name16, Name name17, Name name18,
		Name name19, Name name20, Name name21, Name name22) {
		return context.with(name, name1, name2, name3, name4, name5, name6, name7, name8, name9,
			name10,
			name11, name12, name13, name14, name15, name16, name17, name18, name19, name20, name21,
			name22);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithStep with(CommonTableExpression<?>... commonTableExpressions) {
		return context.with(commonTableExpressions);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep withRecursive(String s) {
		return context.withRecursive(s);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep withRecursive(String s, String... strings) {
		return context.withRecursive(s, strings);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep withRecursive(Name name) {
		return context.withRecursive(name);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep withRecursive(Name name, Name... names) {
		return context.withRecursive(name, names);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep withRecursive(String s,
		Function<? super Field<?>, ? extends String> function) {
		return context.withRecursive(s, function);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL,
		SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep withRecursive(String s,
		BiFunction<? super Field<?>, ? super Integer, ? extends String> biFunction) {
		return context.withRecursive(s, biFunction);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep1 withRecursive(String s, String s1) {
		return context.withRecursive(s, s1);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep2 withRecursive(String s, String s1, String s2) {
		return context.withRecursive(s, s1, s2);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep3 withRecursive(String s, String s1, String s2, String s3) {
		return context.withRecursive(s, s1, s2, s3);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep4 withRecursive(String s, String s1, String s2, String s3,
		String s4) {
		return context.withRecursive(s, s1, s2, s3, s4);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep5 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5) {
		return context.withRecursive(s, s1, s2, s3, s4, s5);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep6 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep7 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep8 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep9 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep10 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9, String s10) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep11 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9, String s10,
		String s11) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep12 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9, String s10,
		String s11, String s12) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep13 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9, String s10,
		String s11, String s12, String s13) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep14 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9, String s10,
		String s11, String s12, String s13, String s14) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13,
			s14);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep15 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9, String s10,
		String s11, String s12, String s13, String s14, String s15) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14,
			s15);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep16 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9, String s10,
		String s11, String s12, String s13, String s14, String s15, String s16) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14,
			s15,
			s16);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep17 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9, String s10,
		String s11, String s12, String s13, String s14, String s15, String s16, String s17) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14,
			s15,
			s16, s17);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep18 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9, String s10,
		String s11, String s12, String s13, String s14, String s15, String s16, String s17,
		String s18) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14,
			s15,
			s16, s17, s18);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep19 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9, String s10,
		String s11, String s12, String s13, String s14, String s15, String s16, String s17,
		String s18, String s19) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14,
			s15,
			s16, s17, s18, s19);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep20 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9, String s10,
		String s11, String s12, String s13, String s14, String s15, String s16, String s17,
		String s18, String s19, String s20) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14,
			s15,
			s16, s17, s18, s19, s20);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep21 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9, String s10,
		String s11, String s12, String s13, String s14, String s15, String s16, String s17,
		String s18, String s19, String s20, String s21) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14,
			s15,
			s16, s17, s18, s19, s20, s21);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep22 withRecursive(String s, String s1, String s2, String s3,
		String s4, String s5, String s6, String s7, String s8, String s9, String s10,
		String s11, String s12, String s13, String s14, String s15, String s16, String s17,
		String s18, String s19, String s20, String s21, String s22) {
		return context.withRecursive(s, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14,
			s15,
			s16, s17, s18, s19, s20, s21, s22);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep1 withRecursive(Name name, Name name1) {
		return context.withRecursive(name, name1);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep2 withRecursive(Name name, Name name1,
		Name name2) {
		return context.withRecursive(name, name1, name2);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep3 withRecursive(Name name, Name name1,
		Name name2, Name name3) {
		return context.withRecursive(name, name1, name2, name3);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep4 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4) {
		return context.withRecursive(name, name1, name2, name3, name4);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep5 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5) {
		return context.withRecursive(name, name1, name2, name3, name4, name5);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep6 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep7 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep8 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep9 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep10 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9,
		Name name10) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9,
			name10);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep11 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9,
		Name name10, Name name11) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9,
			name10, name11);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep12 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9,
		Name name10, Name name11, Name name12) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9,
			name10, name11, name12);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep13 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9,
		Name name10, Name name11, Name name12, Name name13) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9,
			name10, name11, name12, name13);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep14 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9,
		Name name10, Name name11, Name name12, Name name13,
		Name name14) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9,
			name10, name11, name12, name13, name14);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep15 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9,
		Name name10, Name name11, Name name12, Name name13,
		Name name14, Name name15) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9,
			name10, name11, name12, name13, name14, name15);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep16 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9,
		Name name10, Name name11, Name name12, Name name13,
		Name name14, Name name15, Name name16) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9,
			name10, name11, name12, name13, name14, name15, name16);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep17 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9,
		Name name10, Name name11, Name name12, Name name13,
		Name name14, Name name15, Name name16, Name name17) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9,
			name10, name11, name12, name13, name14, name15, name16, name17);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep18 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9,
		Name name10, Name name11, Name name12, Name name13,
		Name name14, Name name15, Name name16, Name name17,
		Name name18) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9,
			name10, name11, name12, name13, name14, name15, name16, name17, name18);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep19 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9,
		Name name10, Name name11, Name name12, Name name13,
		Name name14, Name name15, Name name16, Name name17,
		Name name18, Name name19) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9,
			name10, name11, name12, name13, name14, name15, name16, name17, name18, name19);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep20 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9,
		Name name10, Name name11, Name name12, Name name13,
		Name name14, Name name15, Name name16, Name name17,
		Name name18, Name name19, Name name20) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9,
			name10, name11, name12, name13, name14, name15, name16, name17, name18, name19, name20);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep21 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9,
		Name name10, Name name11, Name name12, Name name13,
		Name name14, Name name15, Name name16, Name name17,
		Name name18, Name name19, Name name20, Name name21) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9,
			name10, name11, name12, name13, name14, name15, name16, name17, name18, name19, name20,
			name21);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithAsStep22 withRecursive(Name name, Name name1,
		Name name2, Name name3, Name name4, Name name5,
		Name name6, Name name7, Name name8, Name name9,
		Name name10, Name name11, Name name12, Name name13,
		Name name14, Name name15, Name name16, Name name17,
		Name name18, Name name19, Name name20, Name name21,
		Name name22) {
		return context.withRecursive(name, name1, name2, name3, name4, name5, name6, name7, name8,
			name9,
			name10, name11, name12, name13, name14, name15, name16, name17, name18, name19, name20,
			name21,
			name22);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public WithStep withRecursive(CommonTableExpression<?>... commonTableExpressions) {
		return context.withRecursive(commonTableExpressions);
	}

	@Override
	@Support
	public <R extends Record> SelectWhereStep<R> selectFrom(Table<R> table) {
		return context.selectFrom(table);
	}

	@Override
	@Support
	public <R extends Record> SelectWhereStep<R> selectFrom(Name name) {
		return context.selectFrom(name);
	}

	@Override
	@PlainSQL
	@Support
	public <R extends Record> SelectWhereStep<R> selectFrom(SQL sql) {
		return context.selectFrom(sql);
	}

	@Override
	@PlainSQL
	@Support
	public <R extends Record> SelectWhereStep<R> selectFrom(String s) {
		return context.selectFrom(s);
	}

	@Override
	@PlainSQL
	@Support
	public <R extends Record> SelectWhereStep<R> selectFrom(String s,
		Object... objects) {
		return context.selectFrom(s, objects);
	}

	@Override
	@PlainSQL
	@Support
	public <R extends Record> SelectWhereStep<R> selectFrom(String s,
		QueryPart... queryParts) {
		return context.selectFrom(s, queryParts);
	}

	@Override
	@Support
	public SelectSelectStep<Record> select(
		Collection<? extends SelectFieldOrAsterisk> collection) {
		return context.select(collection);
	}

	@Override
	@Support
	public SelectSelectStep<Record> select(
		SelectFieldOrAsterisk... selectFieldOrAsterisks) {
		return context.select(selectFieldOrAsterisks);
	}

	@Override
	@Support
	public <T1> SelectSelectStep<Record1<T1>> select(
		SelectField<T1> selectField) {
		return context.select(selectField);
	}

	@Override
	@Support
	public <T1, T2> SelectSelectStep<Record2<T1, T2>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1) {
		return context.select(selectField, selectField1);
	}

	@Override
	@Support
	public <T1, T2, T3> SelectSelectStep<Record3<T1, T2, T3>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2) {
		return context.select(selectField, selectField1, selectField2);
	}

	@Override
	@Support
	public <T1, T2, T3, T4> SelectSelectStep<Record4<T1, T2, T3, T4>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3) {
		return context.select(selectField, selectField1, selectField2, selectField3);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5> SelectSelectStep<Record5<T1, T2, T3, T4, T5>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6> SelectSelectStep<Record6<T1, T2, T3, T4, T5, T6>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7> SelectSelectStep<Record7<T1, T2, T3, T4, T5, T6, T7>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8> SelectSelectStep<Record8<T1, T2, T3, T4, T5, T6, T7, T8>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9> SelectSelectStep<Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> SelectSelectStep<Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> SelectSelectStep<Record11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> SelectSelectStep<Record12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> SelectSelectStep<Record13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> SelectSelectStep<Record14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> SelectSelectStep<Record15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> SelectSelectStep<Record16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> SelectSelectStep<Record17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> SelectSelectStep<Record18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> SelectSelectStep<Record19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17,
		SelectField<T19> selectField18) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17, selectField18);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> SelectSelectStep<Record20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17,
		SelectField<T19> selectField18, SelectField<T20> selectField19) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17, selectField18, selectField19);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> SelectSelectStep<Record21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17,
		SelectField<T19> selectField18, SelectField<T20> selectField19,
		SelectField<T21> selectField20) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17, selectField18, selectField19, selectField20);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> SelectSelectStep<Record22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>> select(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17,
		SelectField<T19> selectField18, SelectField<T20> selectField19,
		SelectField<T21> selectField20, SelectField<T22> selectField21) {
		return context.select(selectField, selectField1, selectField2, selectField3, selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17, selectField18, selectField19, selectField20, selectField21);
	}

	@Override
	@Support
	public SelectSelectStep<Record> selectDistinct(
		Collection<? extends SelectFieldOrAsterisk> collection) {
		return context.selectDistinct(collection);
	}

	@Override
	@Support
	public SelectSelectStep<Record> selectDistinct(
		SelectFieldOrAsterisk... selectFieldOrAsterisks) {
		return context.selectDistinct(selectFieldOrAsterisks);
	}

	@Override
	@Support
	public <T1> SelectSelectStep<Record1<T1>> selectDistinct(
		SelectField<T1> selectField) {
		return context.selectDistinct(selectField);
	}

	@Override
	@Support
	public <T1, T2> SelectSelectStep<Record2<T1, T2>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1) {
		return context.selectDistinct(selectField, selectField1);
	}

	@Override
	@Support
	public <T1, T2, T3> SelectSelectStep<Record3<T1, T2, T3>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2) {
		return context.selectDistinct(selectField, selectField1, selectField2);
	}

	@Override
	@Support
	public <T1, T2, T3, T4> SelectSelectStep<Record4<T1, T2, T3, T4>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5> SelectSelectStep<Record5<T1, T2, T3, T4, T5>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6> SelectSelectStep<Record6<T1, T2, T3, T4, T5, T6>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7> SelectSelectStep<Record7<T1, T2, T3, T4, T5, T6, T7>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8> SelectSelectStep<Record8<T1, T2, T3, T4, T5, T6, T7, T8>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9> SelectSelectStep<Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> SelectSelectStep<Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> SelectSelectStep<Record11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> SelectSelectStep<Record12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> SelectSelectStep<Record13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> SelectSelectStep<Record14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> SelectSelectStep<Record15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> SelectSelectStep<Record16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> SelectSelectStep<Record17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> SelectSelectStep<Record18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> SelectSelectStep<Record19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17,
		SelectField<T19> selectField18) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17, selectField18);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> SelectSelectStep<Record20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17,
		SelectField<T19> selectField18, SelectField<T20> selectField19) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17, selectField18, selectField19);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> SelectSelectStep<Record21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17,
		SelectField<T19> selectField18, SelectField<T20> selectField19,
		SelectField<T21> selectField20) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17, selectField18, selectField19, selectField20);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> SelectSelectStep<Record22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>> selectDistinct(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17,
		SelectField<T19> selectField18, SelectField<T20> selectField19,
		SelectField<T21> selectField20, SelectField<T22> selectField21) {
		return context.selectDistinct(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17, selectField18, selectField19, selectField20, selectField21);
	}

	@Override
	@Support
	public SelectSelectStep<Record1<Integer>> selectZero() {
		return context.selectZero();
	}

	@Override
	@Support
	public SelectSelectStep<Record1<Integer>> selectOne() {
		return context.selectOne();
	}

	@Override
	@Support
	public SelectSelectStep<Record1<Integer>> selectCount() {
		return context.selectCount();
	}

	@Override
	@Support
	public SelectQuery<Record> selectQuery() {
		return context.selectQuery();
	}

	@Override
	@Support
	public <R extends Record> SelectQuery<R> selectQuery(
		TableLike<R> tableLike) {
		return context.selectQuery(tableLike);
	}

	@Override
	@Support
	public <R extends Record> InsertQuery<R> insertQuery(Table<R> table) {
		return context.insertQuery(table);
	}

	@Override
	@Support
	public <R extends Record> InsertSetStep<R> insertInto(Table<R> table) {
		return context.insertInto(table);
	}

	@Override
	@Support
	public <R extends Record, T1> InsertValuesStep1<R, T1> insertInto(
		Table<R> table, Field<T1> field) {
		return context.insertInto(table, field);
	}

	@Override
	@Support
	public <R extends Record, T1, T2> InsertValuesStep2<R, T1, T2> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1) {
		return context.insertInto(table, field, field1);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3> InsertValuesStep3<R, T1, T2, T3> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2) {
		return context.insertInto(table, field, field1, field2);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4> InsertValuesStep4<R, T1, T2, T3, T4> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3) {
		return context.insertInto(table, field, field1, field2, field3);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5> InsertValuesStep5<R, T1, T2, T3, T4, T5> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4) {
		return context.insertInto(table, field, field1, field2, field3, field4);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6> InsertValuesStep6<R, T1, T2, T3, T4, T5, T6> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7> InsertValuesStep7<R, T1, T2, T3, T4, T5, T6, T7> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8> InsertValuesStep8<R, T1, T2, T3, T4, T5, T6, T7, T8> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9> InsertValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> InsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> InsertValuesStep11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> InsertValuesStep12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> InsertValuesStep13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> InsertValuesStep14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> InsertValuesStep15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> InsertValuesStep16<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> InsertValuesStep17<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15, Field<T17> field16) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15, field16);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> InsertValuesStep18<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15, Field<T17> field16,
		Field<T18> field17) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15, field16, field17);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> InsertValuesStep19<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15, Field<T17> field16,
		Field<T18> field17, Field<T19> field18) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15, field16, field17,
			field18);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> InsertValuesStep20<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15, Field<T17> field16,
		Field<T18> field17, Field<T19> field18, Field<T20> field19) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15, field16, field17,
			field18,
			field19);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> InsertValuesStep21<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15, Field<T17> field16,
		Field<T18> field17, Field<T19> field18, Field<T20> field19,
		Field<T21> field20) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15, field16, field17,
			field18,
			field19, field20);
	}

	@Override
	@Support
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> InsertValuesStep22<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> insertInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15, Field<T17> field16,
		Field<T18> field17, Field<T19> field18, Field<T20> field19,
		Field<T21> field20, Field<T22> field21) {
		return context.insertInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15, field16, field17,
			field18,
			field19, field20, field21);
	}

	@Override
	@Support
	public <R extends Record> InsertValuesStepN<R> insertInto(Table<R> table,
		Field<?>... fields) {
		return context.insertInto(table, fields);
	}

	@Override
	@Support
	public <R extends Record> InsertValuesStepN<R> insertInto(Table<R> table,
		Collection<? extends Field<?>> collection) {
		return context.insertInto(table, collection);
	}

	@Override
	@Support
	public <R extends Record> UpdateQuery<R> updateQuery(Table<R> table) {
		return context.updateQuery(table);
	}

	@Override
	@Support
	public <R extends Record> UpdateSetFirstStep<R> update(Table<R> table) {
		return context.update(table);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record> MergeUsingStep<R> mergeInto(Table<R> table) {
		return context.mergeInto(table);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1> MergeKeyStep1<R, T1> mergeInto(
		Table<R> table, Field<T1> field) {
		return context.mergeInto(table, field);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2> MergeKeyStep2<R, T1, T2> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1) {
		return context.mergeInto(table, field, field1);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3> MergeKeyStep3<R, T1, T2, T3> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2) {
		return context.mergeInto(table, field, field1, field2);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4> MergeKeyStep4<R, T1, T2, T3, T4> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3) {
		return context.mergeInto(table, field, field1, field2, field3);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5> MergeKeyStep5<R, T1, T2, T3, T4, T5> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4) {
		return context.mergeInto(table, field, field1, field2, field3, field4);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6> MergeKeyStep6<R, T1, T2, T3, T4, T5, T6> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7> MergeKeyStep7<R, T1, T2, T3, T4, T5, T6, T7> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8> MergeKeyStep8<R, T1, T2, T3, T4, T5, T6, T7, T8> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9> MergeKeyStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> MergeKeyStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> MergeKeyStep11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> MergeKeyStep12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> MergeKeyStep13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> MergeKeyStep14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> MergeKeyStep15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> MergeKeyStep16<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> MergeKeyStep17<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15, Field<T17> field16) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15, field16);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> MergeKeyStep18<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15, Field<T17> field16,
		Field<T18> field17) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15, field16, field17);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> MergeKeyStep19<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15, Field<T17> field16,
		Field<T18> field17, Field<T19> field18) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15, field16, field17,
			field18);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> MergeKeyStep20<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15, Field<T17> field16,
		Field<T18> field17, Field<T19> field18, Field<T20> field19) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15, field16, field17,
			field18,
			field19);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> MergeKeyStep21<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15, Field<T17> field16,
		Field<T18> field17, Field<T19> field18, Field<T20> field19,
		Field<T21> field20) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15, field16, field17,
			field18,
			field19, field20);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> MergeKeyStep22<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> mergeInto(
		Table<R> table, Field<T1> field, Field<T2> field1,
		Field<T3> field2, Field<T4> field3, Field<T5> field4,
		Field<T6> field5, Field<T7> field6, Field<T8> field7,
		Field<T9> field8, Field<T10> field9, Field<T11> field10,
		Field<T12> field11, Field<T13> field12, Field<T14> field13,
		Field<T15> field14, Field<T16> field15, Field<T17> field16,
		Field<T18> field17, Field<T19> field18, Field<T20> field19,
		Field<T21> field20, Field<T22> field21) {
		return context.mergeInto(table, field, field1, field2, field3, field4, field5, field6,
			field7,
			field8, field9, field10, field11, field12, field13, field14, field15, field16, field17,
			field18,
			field19, field20, field21);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB,
		SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record> MergeKeyStepN<R> mergeInto(Table<R> table,
		Field<?>... fields) {
		return context.mergeInto(table, fields);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB,
		SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public <R extends Record> MergeKeyStepN<R> mergeInto(Table<R> table,
		Collection<? extends Field<?>> collection) {
		return context.mergeInto(table, collection);
	}

	@Override
	@Support
	public <R extends Record> DeleteQuery<R> deleteQuery(Table<R> table) {
		return context.deleteQuery(table);
	}

	@Override
	@Support
	public <R extends Record> DeleteWhereStep<R> deleteFrom(Table<R> table) {
		return context.deleteFrom(table);
	}

	@Override
	@Support
	public <R extends Record> DeleteWhereStep<R> delete(Table<R> table) {
		return context.delete(table);
	}

	@Override
	@Support
	public Batch batch(Query... queries) {
		return context.batch(queries);
	}

	@Override
	@Support
	public Batch batch(Queries queries) {
		return context.batch(queries);
	}

	@Override
	@Support
	public Batch batch(String... strings) {
		return context.batch(strings);
	}

	@Override
	@Support
	public Batch batch(Collection<? extends Query> collection) {
		return context.batch(collection);
	}

	@Override
	@Support
	public BatchBindStep batch(Query query) {
		return context.batch(query);
	}

	@Override
	@Support
	public BatchBindStep batch(String s) {
		return context.batch(s);
	}

	@Override
	@Support
	public Batch batch(Query query, Object[]... objects) {
		return context.batch(query, objects);
	}

	@Override
	@Support
	public Batch batch(String s, Object[]... objects) {
		return context.batch(s, objects);
	}

	@Override
	@Support
	public Batch batchStore(UpdatableRecord<?>... updatableRecords) {
		return context.batchStore(updatableRecords);
	}

	@Override
	@Support
	public Batch batchStore(
		Collection<? extends UpdatableRecord<?>> collection) {
		return context.batchStore(collection);
	}

	@Override
	@Support
	public Batch batchInsert(TableRecord<?>... tableRecords) {
		return context.batchInsert(tableRecords);
	}

	@Override
	@Support
	public Batch batchInsert(
		Collection<? extends TableRecord<?>> collection) {
		return context.batchInsert(collection);
	}

	@Override
	@Support
	public Batch batchUpdate(UpdatableRecord<?>... updatableRecords) {
		return context.batchUpdate(updatableRecords);
	}

	@Override
	@Support
	public Batch batchUpdate(
		Collection<? extends UpdatableRecord<?>> collection) {
		return context.batchUpdate(collection);
	}

	@Override
	@Support
	public Batch batchDelete(UpdatableRecord<?>... updatableRecords) {
		return context.batchDelete(updatableRecords);
	}

	@Override
	@Support
	public Batch batchDelete(
		Collection<? extends UpdatableRecord<?>> collection) {
		return context.batchDelete(collection);
	}

	@Override
	public Queries ddl(Catalog catalog) {
		return context.ddl(catalog);
	}

	@Override
	public Queries ddl(Catalog catalog,
		DDLExportConfiguration ddlExportConfiguration) {
		return context.ddl(catalog, ddlExportConfiguration);
	}

	@Override
	public Queries ddl(Catalog catalog, DDLFlag... ddlFlags) {
		return context.ddl(catalog, ddlFlags);
	}

	@Override
	public Queries ddl(Schema schema) {
		return context.ddl(schema);
	}

	@Override
	public Queries ddl(Schema schema,
		DDLExportConfiguration ddlExportConfiguration) {
		return context.ddl(schema, ddlExportConfiguration);
	}

	@Override
	public Queries ddl(Schema schema, DDLFlag... ddlFlags) {
		return context.ddl(schema, ddlFlags);
	}

	@Override
	public Queries ddl(Table<?> table) {
		return context.ddl(table);
	}

	@Override
	public Queries ddl(Table<?> table,
		DDLExportConfiguration ddlExportConfiguration) {
		return context.ddl(table, ddlExportConfiguration);
	}

	@Override
	public Queries ddl(Table<?> table, DDLFlag... ddlFlags) {
		return context.ddl(table, ddlFlags);
	}

	@Override
	public Queries ddl(Table<?>... tables) {
		return context.ddl(tables);
	}

	@Override
	public Queries ddl(Table<?>[] tables,
		DDLExportConfiguration ddlExportConfiguration) {
		return context.ddl(tables, ddlExportConfiguration);
	}

	@Override
	public Queries ddl(Table<?>[] tables, DDLFlag... ddlFlags) {
		return context.ddl(tables, ddlFlags);
	}

	@Override
	public Queries ddl(Collection<? extends Table<?>> collection) {
		return context.ddl(collection);
	}

	@Override
	public Queries ddl(Collection<? extends Table<?>> collection,
		DDLFlag... ddlFlags) {
		return context.ddl(collection, ddlFlags);
	}

	@Override
	public Queries ddl(Collection<? extends Table<?>> collection,
		DDLExportConfiguration ddlExportConfiguration) {
		return context.ddl(collection, ddlExportConfiguration);
	}

	@Override
	@Support({SQLDialect.MARIADB, SQLDialect.MYSQL})
	public RowCountQuery setCatalog(String s) {
		return context.setCatalog(s);
	}

	@Override
	@Support({SQLDialect.MARIADB, SQLDialect.MYSQL})
	public RowCountQuery setCatalog(Name name) {
		return context.setCatalog(name);
	}

	@Override
	@Support({SQLDialect.MARIADB, SQLDialect.MYSQL})
	public RowCountQuery setCatalog(Catalog catalog) {
		return context.setCatalog(catalog);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public RowCountQuery setSchema(String s) {
		return context.setSchema(s);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public RowCountQuery setSchema(Name name) {
		return context.setSchema(name);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public RowCountQuery setSchema(Schema schema) {
		return context.setSchema(schema);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CommentOnIsStep commentOnTable(String s) {
		return context.commentOnTable(s);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CommentOnIsStep commentOnTable(Name name) {
		return context.commentOnTable(name);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CommentOnIsStep commentOnTable(Table<?> table) {
		return context.commentOnTable(table);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public CommentOnIsStep commentOnView(String s) {
		return context.commentOnView(s);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public CommentOnIsStep commentOnView(Name name) {
		return context.commentOnView(name);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public CommentOnIsStep commentOnView(Table<?> table) {
		return context.commentOnView(table);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public CommentOnIsStep commentOnColumn(Name name) {
		return context.commentOnColumn(name);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public CommentOnIsStep commentOnColumn(Field<?> field) {
		return context.commentOnColumn(field);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateSchemaFinalStep createSchema(String s) {
		return context.createSchema(s);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateSchemaFinalStep createSchema(Name name) {
		return context.createSchema(name);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateSchemaFinalStep createSchema(Schema schema) {
		return context.createSchema(schema);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateSchemaFinalStep createSchemaIfNotExists(String s) {
		return context.createSchemaIfNotExists(s);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateSchemaFinalStep createSchemaIfNotExists(Name name) {
		return context.createSchemaIfNotExists(name);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateSchemaFinalStep createSchemaIfNotExists(Schema schema) {
		return context.createSchemaIfNotExists(schema);
	}

	@Override
	@Support
	public CreateTableColumnStep createTable(String s) {
		return context.createTable(s);
	}

	@Override
	@Support
	public CreateTableColumnStep createTable(Name name) {
		return context.createTable(name);
	}

	@Override
	@Support
	public CreateTableColumnStep createTable(Table<?> table) {
		return context.createTable(table);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public CreateTableColumnStep createTableIfNotExists(String s) {
		return context.createTableIfNotExists(s);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public CreateTableColumnStep createTableIfNotExists(Name name) {
		return context.createTableIfNotExists(name);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public CreateTableColumnStep createTableIfNotExists(Table<?> table) {
		return context.createTableIfNotExists(table);
	}

	@Override
	@Support({SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateTableColumnStep createTemporaryTable(String s) {
		return context.createTemporaryTable(s);
	}

	@Override
	@Support({SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateTableColumnStep createTemporaryTable(Name name) {
		return context.createTemporaryTable(name);
	}

	@Override
	@Support({SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateTableColumnStep createTemporaryTable(Table<?> table) {
		return context.createTemporaryTable(table);
	}

	@Override
	@Support({SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateTableColumnStep createGlobalTemporaryTable(String s) {
		return context.createGlobalTemporaryTable(s);
	}

	@Override
	@Support({SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateTableColumnStep createGlobalTemporaryTable(Name name) {
		return context.createGlobalTemporaryTable(name);
	}

	@Override
	@Support({SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateTableColumnStep createGlobalTemporaryTable(Table<?> table) {
		return context.createGlobalTemporaryTable(table);
	}

	@Override
	@Support
	public CreateViewAsStep<Record> createView(String s, String... strings) {
		return context.createView(s, strings);
	}

	@Override
	@Support
	public CreateViewAsStep<Record> createView(Name name,
		Name... names) {
		return context.createView(name, names);
	}

	@Override
	@Support
	public CreateViewAsStep<Record> createView(Table<?> table,
		Field<?>... fields) {
		return context.createView(table, fields);
	}

	@Override
	@Support
	public CreateViewAsStep<Record> createView(String s,
		Function<? super Field<?>, ? extends String> function) {
		return context.createView(s, function);
	}

	@Override
	@Support
	public CreateViewAsStep<Record> createView(String s,
		BiFunction<? super Field<?>, ? super Integer, ? extends String> biFunction) {
		return context.createView(s, biFunction);
	}

	@Override
	@Support
	public CreateViewAsStep<Record> createView(Name name,
		Function<? super Field<?>, ? extends Name> function) {
		return context.createView(name, function);
	}

	@Override
	@Support
	public CreateViewAsStep<Record> createView(Name name,
		BiFunction<? super Field<?>, ? super Integer, ? extends Name> biFunction) {
		return context.createView(name, biFunction);
	}

	@Override
	@Support
	public CreateViewAsStep<Record> createView(Table<?> table,
		Function<? super Field<?>, ? extends Field<?>> function) {
		return context.createView(table, function);
	}

	@Override
	@Support
	public CreateViewAsStep<Record> createView(Table<?> table,
		BiFunction<? super Field<?>, ? super Integer, ? extends Field<?>> biFunction) {
		return context.createView(table, biFunction);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateViewAsStep<Record> createOrReplaceView(String s,
		String... strings) {
		return context.createOrReplaceView(s, strings);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateViewAsStep<Record> createOrReplaceView(Name name,
		Name... names) {
		return context.createOrReplaceView(name, names);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateViewAsStep<Record> createOrReplaceView(Table<?> table,
		Field<?>... fields) {
		return context.createOrReplaceView(table, fields);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateViewAsStep<Record> createOrReplaceView(String s,
		Function<? super Field<?>, ? extends String> function) {
		return context.createOrReplaceView(s, function);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateViewAsStep<Record> createOrReplaceView(String s,
		BiFunction<? super Field<?>, ? super Integer, ? extends String> biFunction) {
		return context.createOrReplaceView(s, biFunction);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateViewAsStep<Record> createOrReplaceView(Name name,
		Function<? super Field<?>, ? extends Name> function) {
		return context.createOrReplaceView(name, function);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateViewAsStep<Record> createOrReplaceView(Name name,
		BiFunction<? super Field<?>, ? super Integer, ? extends Name> biFunction) {
		return context.createOrReplaceView(name, biFunction);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateViewAsStep<Record> createOrReplaceView(Table<?> table,
		Function<? super Field<?>, ? extends Field<?>> function) {
		return context.createOrReplaceView(table, function);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public CreateViewAsStep<Record> createOrReplaceView(Table<?> table,
		BiFunction<? super Field<?>, ? super Integer, ? extends Field<?>> biFunction) {
		return context.createOrReplaceView(table, biFunction);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public CreateViewAsStep<Record> createViewIfNotExists(String s,
		String... strings) {
		return context.createViewIfNotExists(s, strings);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public CreateViewAsStep<Record> createViewIfNotExists(Name name,
		Name... names) {
		return context.createViewIfNotExists(name, names);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public CreateViewAsStep<Record> createViewIfNotExists(Table<?> table,
		Field<?>... fields) {
		return context.createViewIfNotExists(table, fields);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public CreateViewAsStep<Record> createViewIfNotExists(String s,
		Function<? super Field<?>, ? extends String> function) {
		return context.createViewIfNotExists(s, function);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public CreateViewAsStep<Record> createViewIfNotExists(String s,
		BiFunction<? super Field<?>, ? super Integer, ? extends String> biFunction) {
		return context.createViewIfNotExists(s, biFunction);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public CreateViewAsStep<Record> createViewIfNotExists(Name name,
		Function<? super Field<?>, ? extends Name> function) {
		return context.createViewIfNotExists(name, function);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public CreateViewAsStep<Record> createViewIfNotExists(Name name,
		BiFunction<? super Field<?>, ? super Integer, ? extends Name> biFunction) {
		return context.createViewIfNotExists(name, biFunction);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public CreateViewAsStep<Record> createViewIfNotExists(Table<?> table,
		Function<? super Field<?>, ? extends Field<?>> function) {
		return context.createViewIfNotExists(table, function);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public CreateViewAsStep<Record> createViewIfNotExists(Table<?> table,
		BiFunction<? super Field<?>, ? super Integer, ? extends Field<?>> biFunction) {
		return context.createViewIfNotExists(table, biFunction);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public CreateTypeStep createType(String s) {
		return context.createType(s);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public CreateTypeStep createType(Name name) {
		return context.createType(name);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public DropTypeStep dropType(String s) {
		return context.dropType(s);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public DropTypeStep dropType(Name name) {
		return context.dropType(name);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public DropTypeStep dropType(String... strings) {
		return context.dropType(strings);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public DropTypeStep dropType(Name... names) {
		return context.dropType(names);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public DropTypeStep dropType(Collection<?> collection) {
		return context.dropType(collection);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public DropTypeStep dropTypeIfExists(String s) {
		return context.dropTypeIfExists(s);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public DropTypeStep dropTypeIfExists(Name name) {
		return context.dropTypeIfExists(name);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public DropTypeStep dropTypeIfExists(String... strings) {
		return context.dropTypeIfExists(strings);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public DropTypeStep dropTypeIfExists(Name... names) {
		return context.dropTypeIfExists(names);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public DropTypeStep dropTypeIfExists(Collection<?> collection) {
		return context.dropTypeIfExists(collection);
	}

	@Override
	@Support
	public CreateIndexStep createIndex() {
		return context.createIndex();
	}

	@Override
	@Support
	public CreateIndexStep createIndex(String s) {
		return context.createIndex(s);
	}

	@Override
	@Support
	public CreateIndexStep createIndex(Name name) {
		return context.createIndex(name);
	}

	@Override
	@Support
	public CreateIndexStep createIndex(Index index) {
		return context.createIndex(index);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES,
		SQLDialect.SQLITE})
	public CreateIndexStep createIndexIfNotExists(String s) {
		return context.createIndexIfNotExists(s);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES,
		SQLDialect.SQLITE})
	public CreateIndexStep createIndexIfNotExists(Name name) {
		return context.createIndexIfNotExists(name);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES,
		SQLDialect.SQLITE})
	public CreateIndexStep createIndexIfNotExists(Index index) {
		return context.createIndexIfNotExists(index);
	}

	@Override
	@Support
	public CreateIndexStep createUniqueIndex() {
		return context.createUniqueIndex();
	}

	@Override
	@Support
	public CreateIndexStep createUniqueIndex(String s) {
		return context.createUniqueIndex(s);
	}

	@Override
	@Support
	public CreateIndexStep createUniqueIndex(Name name) {
		return context.createUniqueIndex(name);
	}

	@Override
	@Support
	public CreateIndexStep createUniqueIndex(Index index) {
		return context.createUniqueIndex(index);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES,
		SQLDialect.SQLITE})
	public CreateIndexStep createUniqueIndexIfNotExists(String s) {
		return context.createUniqueIndexIfNotExists(s);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES,
		SQLDialect.SQLITE})
	public CreateIndexStep createUniqueIndexIfNotExists(Name name) {
		return context.createUniqueIndexIfNotExists(name);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES,
		SQLDialect.SQLITE})
	public CreateIndexStep createUniqueIndexIfNotExists(Index index) {
		return context.createUniqueIndexIfNotExists(index);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public CreateSequenceFlagsStep createSequence(String s) {
		return context.createSequence(s);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public CreateSequenceFlagsStep createSequence(Name name) {
		return context.createSequence(name);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public CreateSequenceFlagsStep createSequence(Sequence<?> sequence) {
		return context.createSequence(sequence);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public CreateSequenceFlagsStep createSequenceIfNotExists(String s) {
		return context.createSequenceIfNotExists(s);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public CreateSequenceFlagsStep createSequenceIfNotExists(Name name) {
		return context.createSequenceIfNotExists(name);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public CreateSequenceFlagsStep createSequenceIfNotExists(Sequence<?> sequence) {
		return context.createSequenceIfNotExists(sequence);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB,
		SQLDialect.POSTGRES})
	public AlterSequenceStep<BigInteger> alterSequence(String s) {
		return context.alterSequence(s);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB,
		SQLDialect.POSTGRES})
	public AlterSequenceStep<BigInteger> alterSequence(Name name) {
		return context.alterSequence(name);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB,
		SQLDialect.POSTGRES})
	public <T extends Number> AlterSequenceStep<T> alterSequence(Sequence<T> sequence) {
		return context.alterSequence(sequence);
	}

	@Override
	@Support({SQLDialect.POSTGRES})
	public AlterSequenceStep<BigInteger> alterSequenceIfExists(String s) {
		return context.alterSequenceIfExists(s);
	}

	@Override
	@Support({SQLDialect.POSTGRES})
	public AlterSequenceStep<BigInteger> alterSequenceIfExists(Name name) {
		return context.alterSequenceIfExists(name);
	}

	@Override
	@Support({SQLDialect.POSTGRES})
	public <T extends Number> AlterSequenceStep<T> alterSequenceIfExists(
		Sequence<T> sequence) {
		return context.alterSequenceIfExists(sequence);
	}

	@Override
	@Support
	public AlterTableStep alterTable(String s) {
		return context.alterTable(s);
	}

	@Override
	@Support
	public AlterTableStep alterTable(Name name) {
		return context.alterTable(name);
	}

	@Override
	@Support
	public AlterTableStep alterTable(Table<?> table) {
		return context.alterTable(table);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MARIADB, SQLDialect.POSTGRES})
	public AlterTableStep alterTableIfExists(String s) {
		return context.alterTableIfExists(s);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MARIADB, SQLDialect.POSTGRES})
	public AlterTableStep alterTableIfExists(Name name) {
		return context.alterTableIfExists(name);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.MARIADB, SQLDialect.POSTGRES})
	public AlterTableStep alterTableIfExists(Table<?> table) {
		return context.alterTableIfExists(table);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public AlterSchemaStep alterSchema(String s) {
		return context.alterSchema(s);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public AlterSchemaStep alterSchema(Name name) {
		return context.alterSchema(name);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public AlterSchemaStep alterSchema(Schema schema) {
		return context.alterSchema(schema);
	}

	@Override
	@Support({SQLDialect.H2})
	public AlterSchemaStep alterSchemaIfExists(String s) {
		return context.alterSchemaIfExists(s);
	}

	@Override
	@Support({SQLDialect.H2})
	public AlterSchemaStep alterSchemaIfExists(Name name) {
		return context.alterSchemaIfExists(name);
	}

	@Override
	@Support({SQLDialect.H2})
	public AlterSchemaStep alterSchemaIfExists(Schema schema) {
		return context.alterSchemaIfExists(schema);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public AlterViewStep alterView(String s) {
		return context.alterView(s);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public AlterViewStep alterView(Name name) {
		return context.alterView(name);
	}

	@Override
	@Support({SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public AlterViewStep alterView(Table<?> table) {
		return context.alterView(table);
	}

	@Override
	@Support({SQLDialect.POSTGRES})
	public AlterViewStep alterViewIfExists(String s) {
		return context.alterViewIfExists(s);
	}

	@Override
	@Support({SQLDialect.POSTGRES})
	public AlterViewStep alterViewIfExists(Name name) {
		return context.alterViewIfExists(name);
	}

	@Override
	@Support({SQLDialect.POSTGRES})
	public AlterViewStep alterViewIfExists(Table<?> table) {
		return context.alterViewIfExists(table);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public AlterIndexOnStep alterIndex(String s) {
		return context.alterIndex(s);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public AlterIndexOnStep alterIndex(Name name) {
		return context.alterIndex(name);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public AlterIndexOnStep alterIndex(Index index) {
		return context.alterIndex(index);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public AlterIndexStep alterIndexIfExists(String s) {
		return context.alterIndexIfExists(s);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public AlterIndexStep alterIndexIfExists(Name name) {
		return context.alterIndexIfExists(name);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.POSTGRES})
	public AlterIndexStep alterIndexIfExists(Index index) {
		return context.alterIndexIfExists(index);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public DropSchemaStep dropSchema(String s) {
		return context.dropSchema(s);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public DropSchemaStep dropSchema(Name name) {
		return context.dropSchema(name);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public DropSchemaStep dropSchema(Schema schema) {
		return context.dropSchema(schema);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL,
		SQLDialect.POSTGRES})
	public DropSchemaStep dropSchemaIfExists(String s) {
		return context.dropSchemaIfExists(s);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL,
		SQLDialect.POSTGRES})
	public DropSchemaStep dropSchemaIfExists(Name name) {
		return context.dropSchemaIfExists(name);
	}

	@Override
	@Support({SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.MYSQL,
		SQLDialect.POSTGRES})
	public DropSchemaStep dropSchemaIfExists(Schema schema) {
		return context.dropSchemaIfExists(schema);
	}

	@Override
	@Support
	public DropViewFinalStep dropView(String s) {
		return context.dropView(s);
	}

	@Override
	@Support
	public DropViewFinalStep dropView(Name name) {
		return context.dropView(name);
	}

	@Override
	@Support
	public DropViewFinalStep dropView(Table<?> table) {
		return context.dropView(table);
	}

	@Override
	@Support
	public DropViewFinalStep dropViewIfExists(String s) {
		return context.dropViewIfExists(s);
	}

	@Override
	@Support
	public DropViewFinalStep dropViewIfExists(Name name) {
		return context.dropViewIfExists(name);
	}

	@Override
	@Support
	public DropViewFinalStep dropViewIfExists(Table<?> table) {
		return context.dropViewIfExists(table);
	}

	@Override
	@Support
	public DropTableStep dropTable(String s) {
		return context.dropTable(s);
	}

	@Override
	@Support
	public DropTableStep dropTable(Name name) {
		return context.dropTable(name);
	}

	@Override
	@Support
	public DropTableStep dropTable(Table<?> table) {
		return context.dropTable(table);
	}

	@Override
	@Support
	public DropTableStep dropTableIfExists(String s) {
		return context.dropTableIfExists(s);
	}

	@Override
	@Support
	public DropTableStep dropTableIfExists(Name name) {
		return context.dropTableIfExists(name);
	}

	@Override
	@Support
	public DropTableStep dropTableIfExists(Table<?> table) {
		return context.dropTableIfExists(table);
	}

	@Override
	@Support({SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public DropTableStep dropTemporaryTable(String s) {
		return context.dropTemporaryTable(s);
	}

	@Override
	@Support({SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public DropTableStep dropTemporaryTable(Name name) {
		return context.dropTemporaryTable(name);
	}

	@Override
	@Support({SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public DropTableStep dropTemporaryTable(Table<?> table) {
		return context.dropTemporaryTable(table);
	}

	@Override
	@Support
	public DropIndexOnStep dropIndex(String s) {
		return context.dropIndex(s);
	}

	@Override
	@Support
	public DropIndexOnStep dropIndex(Name name) {
		return context.dropIndex(name);
	}

	@Override
	@Support
	public DropIndexOnStep dropIndex(Index index) {
		return context.dropIndex(index);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public DropIndexOnStep dropIndexIfExists(String s) {
		return context.dropIndexIfExists(s);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public DropIndexOnStep dropIndexIfExists(Name name) {
		return context.dropIndexIfExists(name);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.MARIADB, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public DropIndexOnStep dropIndexIfExists(Index index) {
		return context.dropIndexIfExists(index);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public DropSequenceFinalStep dropSequence(String s) {
		return context.dropSequence(s);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public DropSequenceFinalStep dropSequence(Name name) {
		return context.dropSequence(name);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public DropSequenceFinalStep dropSequence(Sequence<?> sequence) {
		return context.dropSequence(sequence);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB,
		SQLDialect.POSTGRES})
	public DropSequenceFinalStep dropSequenceIfExists(String s) {
		return context.dropSequenceIfExists(s);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB,
		SQLDialect.POSTGRES})
	public DropSequenceFinalStep dropSequenceIfExists(Name name) {
		return context.dropSequenceIfExists(name);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB,
		SQLDialect.POSTGRES})
	public DropSequenceFinalStep dropSequenceIfExists(Sequence<?> sequence) {
		return context.dropSequenceIfExists(sequence);
	}

	@Override
	@Support
	public TruncateIdentityStep<Record> truncate(String s) {
		return context.truncate(s);
	}

	@Override
	@Support
	public TruncateIdentityStep<Record> truncate(Name name) {
		return context.truncate(name);
	}

	@Override
	@Support
	public <R extends Record> TruncateIdentityStep<R> truncate(Table<R> table) {
		return context.truncate(table);
	}

	@Override
	@Support
	public TruncateIdentityStep<Record> truncateTable(String s) {
		return context.truncateTable(s);
	}

	@Override
	@Support
	public TruncateIdentityStep<Record> truncateTable(Name name) {
		return context.truncateTable(name);
	}

	@Override
	@Support
	public <R extends Record> TruncateIdentityStep<R> truncateTable(
		Table<R> table) {
		return context.truncateTable(table);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public GrantOnStep grant(Privilege privilege) {
		return context.grant(privilege);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public GrantOnStep grant(Privilege... privileges) {
		return context.grant(privileges);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public GrantOnStep grant(Collection<? extends Privilege> collection) {
		return context.grant(collection);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public RevokeOnStep revoke(Privilege privilege) {
		return context.revoke(privilege);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public RevokeOnStep revoke(Privilege... privileges) {
		return context.revoke(privileges);
	}

	@Override
	@Support({SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.MARIADB,
		SQLDialect.MYSQL, SQLDialect.POSTGRES})
	public RevokeOnStep revoke(Collection<? extends Privilege> collection) {
		return context.revoke(collection);
	}

	@Override
	@Support({SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public RevokeOnStep revokeGrantOptionFor(Privilege privilege) {
		return context.revokeGrantOptionFor(privilege);
	}

	@Override
	@Support({SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public RevokeOnStep revokeGrantOptionFor(Privilege... privileges) {
		return context.revokeGrantOptionFor(privileges);
	}

	@Override
	@Support({SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public RevokeOnStep revokeGrantOptionFor(
		Collection<? extends Privilege> collection) {
		return context.revokeGrantOptionFor(collection);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.H2, SQLDialect.HSQLDB,
		SQLDialect.MARIADB, SQLDialect.MYSQL, SQLDialect.POSTGRES, SQLDialect.SQLITE})
	public BigInteger lastID() throws DataAccessException {
		return context.lastID();
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public BigInteger nextval(String s) throws DataAccessException {
		return context.nextval(s);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public BigInteger nextval(Name name) throws DataAccessException {
		return context.nextval(name);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.DERBY, SQLDialect.FIREBIRD, SQLDialect.H2,
		SQLDialect.HSQLDB, SQLDialect.POSTGRES})
	public <T extends Number> T nextval(Sequence<T> sequence) throws DataAccessException {
		return context.nextval(sequence);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB,
		SQLDialect.POSTGRES})
	public BigInteger currval(String s) throws DataAccessException {
		return context.currval(s);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB,
		SQLDialect.POSTGRES})
	public BigInteger currval(Name name) throws DataAccessException {
		return context.currval(name);
	}

	@Override
	@Support({SQLDialect.CUBRID, SQLDialect.FIREBIRD, SQLDialect.H2, SQLDialect.HSQLDB,
		SQLDialect.POSTGRES})
	public <T extends Number> T currval(Sequence<T> sequence) throws DataAccessException {
		return context.currval(sequence);
	}

	@Override
	public <R extends UDTRecord<R>> R newRecord(UDT<R> udt) {
		return context.newRecord(udt);
	}

	@Override
	public <R extends Record> R newRecord(Table<R> table) {
		return context.newRecord(table);
	}

	@Override
	public <R extends Record> R newRecord(Table<R> table, Object o) {
		return context.newRecord(table, o);
	}

	@Override
	public Record newRecord(Field<?>... fields) {
		return context.newRecord(fields);
	}

	@Override
	public Record newRecord(Collection<? extends Field<?>> collection) {
		return context.newRecord(collection);
	}

	@Override
	public <T1> Record1<T1> newRecord(Field<T1> field) {
		return context.newRecord(field);
	}

	@Override
	public <T1, T2> Record2<T1, T2> newRecord(Field<T1> field,
		Field<T2> field1) {
		return context.newRecord(field, field1);
	}

	@Override
	public <T1, T2, T3> Record3<T1, T2, T3> newRecord(Field<T1> field,
		Field<T2> field1, Field<T3> field2) {
		return context.newRecord(field, field1, field2);
	}

	@Override
	public <T1, T2, T3, T4> Record4<T1, T2, T3, T4> newRecord(Field<T1> field,
		Field<T2> field1, Field<T3> field2, Field<T4> field3) {
		return context.newRecord(field, field1, field2, field3);
	}

	@Override
	public <T1, T2, T3, T4, T5> Record5<T1, T2, T3, T4, T5> newRecord(Field<T1> field,
		Field<T2> field1, Field<T3> field2, Field<T4> field3,
		Field<T5> field4) {
		return context.newRecord(field, field1, field2, field3, field4);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6> Record6<T1, T2, T3, T4, T5, T6> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5) {
		return context.newRecord(field, field1, field2, field3, field4, field5);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7> Record7<T1, T2, T3, T4, T5, T6, T7> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8> Record8<T1, T2, T3, T4, T5, T6, T7, T8> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9> Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Record11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Record12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Record13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Record14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Record15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Record16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> Record17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15, Field<T17> field16) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15, field16);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> Record18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15, Field<T17> field16, Field<T18> field17) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15, field16, field17);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> Record19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15, Field<T17> field16, Field<T18> field17,
		Field<T19> field18) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15, field16, field17,
			field18);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> Record20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15, Field<T17> field16, Field<T18> field17,
		Field<T19> field18, Field<T20> field19) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15, field16, field17, field18,
			field19);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> Record21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15, Field<T17> field16, Field<T18> field17,
		Field<T19> field18, Field<T20> field19, Field<T21> field20) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15, field16, field17, field18,
			field19, field20);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> Record22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> newRecord(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15, Field<T17> field16, Field<T18> field17,
		Field<T19> field18, Field<T20> field19, Field<T21> field20,
		Field<T22> field21) {
		return context.newRecord(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15, field16, field17, field18,
			field19, field20, field21);
	}

	@Override
	public <R extends Record> Result<R> newResult(Table<R> table) {
		return context.newResult(table);
	}

	@Override
	public Result<Record> newResult(Field<?>... fields) {
		return context.newResult(fields);
	}

	@Override
	public Result<Record> newResult(
		Collection<? extends Field<?>> collection) {
		return context.newResult(collection);
	}

	@Override
	public <T1> Result<Record1<T1>> newResult(Field<T1> field) {
		return context.newResult(field);
	}

	@Override
	public <T1, T2> Result<Record2<T1, T2>> newResult(Field<T1> field,
		Field<T2> field1) {
		return context.newResult(field, field1);
	}

	@Override
	public <T1, T2, T3> Result<Record3<T1, T2, T3>> newResult(Field<T1> field,
		Field<T2> field1, Field<T3> field2) {
		return context.newResult(field, field1, field2);
	}

	@Override
	public <T1, T2, T3, T4> Result<Record4<T1, T2, T3, T4>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3) {
		return context.newResult(field, field1, field2, field3);
	}

	@Override
	public <T1, T2, T3, T4, T5> Result<Record5<T1, T2, T3, T4, T5>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4) {
		return context.newResult(field, field1, field2, field3, field4);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6> Result<Record6<T1, T2, T3, T4, T5, T6>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5) {
		return context.newResult(field, field1, field2, field3, field4, field5);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7> Result<Record7<T1, T2, T3, T4, T5, T6, T7>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8> Result<Record8<T1, T2, T3, T4, T5, T6, T7, T8>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9> Result<Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Result<Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Result<Record11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Result<Record12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Result<Record13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Result<Record14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Result<Record15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Result<Record16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> Result<Record17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15, Field<T17> field16) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15, field16);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> Result<Record18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15, Field<T17> field16, Field<T18> field17) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15, field16, field17);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> Result<Record19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15, Field<T17> field16, Field<T18> field17,
		Field<T19> field18) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15, field16, field17,
			field18);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> Result<Record20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15, Field<T17> field16, Field<T18> field17,
		Field<T19> field18, Field<T20> field19) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15, field16, field17, field18,
			field19);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> Result<Record21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15, Field<T17> field16, Field<T18> field17,
		Field<T19> field18, Field<T20> field19, Field<T21> field20) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15, field16, field17, field18,
			field19, field20);
	}

	@Override
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> Result<Record22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>> newResult(
		Field<T1> field, Field<T2> field1, Field<T3> field2,
		Field<T4> field3, Field<T5> field4, Field<T6> field5,
		Field<T7> field6, Field<T8> field7, Field<T9> field8,
		Field<T10> field9, Field<T11> field10, Field<T12> field11,
		Field<T13> field12, Field<T14> field13, Field<T15> field14,
		Field<T16> field15, Field<T17> field16, Field<T18> field17,
		Field<T19> field18, Field<T20> field19, Field<T21> field20,
		Field<T22> field21) {
		return context.newResult(field, field1, field2, field3, field4, field5, field6, field7,
			field8,
			field9, field10, field11, field12, field13, field14, field15, field16, field17, field18,
			field19, field20, field21);
	}

	@Override
	public <R extends Record> Result<R> fetch(ResultQuery<R> resultQuery)
		throws DataAccessException {
		return context.fetch(resultQuery);
	}

	@Override
	public <R extends Record> Cursor<R> fetchLazy(ResultQuery<R> resultQuery)
		throws DataAccessException {
		return context.fetchLazy(resultQuery);
	}

	@Override
	public <R extends Record> CompletionStage<Result<R>> fetchAsync(
		ResultQuery<R> resultQuery) {
		return context.fetchAsync(resultQuery);
	}

	@Override
	public <R extends Record> CompletionStage<Result<R>> fetchAsync(
		Executor executor, ResultQuery<R> resultQuery) {
		return context.fetchAsync(executor, resultQuery);
	}

	@Override
	public <R extends Record> Stream<R> fetchStream(
		ResultQuery<R> resultQuery) throws DataAccessException {
		return context.fetchStream(resultQuery);
	}

	@Override
	public <R extends Record> Results fetchMany(ResultQuery<R> resultQuery)
		throws DataAccessException {
		return context.fetchMany(resultQuery);
	}

	@Override
	public <R extends Record> R fetchOne(ResultQuery<R> resultQuery)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOne(resultQuery);
	}

	@Override
	public <R extends Record> R fetchSingle(ResultQuery<R> resultQuery)
		throws DataAccessException, NoDataFoundException, TooManyRowsException {
		return context.fetchSingle(resultQuery);
	}

	@Override
	public <R extends Record> Optional<R> fetchOptional(
		ResultQuery<R> resultQuery) throws DataAccessException, TooManyRowsException {
		return context.fetchOptional(resultQuery);
	}

	@Override
	public <T> T fetchValue(Table<? extends Record1<T>> table)
		throws DataAccessException, TooManyRowsException {
		return context.fetchValue(table);
	}

	@Override
	public <T, R extends Record1<T>> T fetchValue(ResultQuery<R> resultQuery)
		throws DataAccessException, TooManyRowsException {
		return context.fetchValue(resultQuery);
	}

	@Override
	public <T> T fetchValue(TableField<?, T> tableField)
		throws DataAccessException, TooManyRowsException {
		return context.fetchValue(tableField);
	}

	@Override
	public <T> T fetchValue(Field<T> field) throws DataAccessException {
		return context.fetchValue(field);
	}

	@Override
	public <T, R extends Record1<T>> Optional<T> fetchOptionalValue(
		ResultQuery<R> resultQuery)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchOptionalValue(resultQuery);
	}

	@Override
	public <T> Optional<T> fetchOptionalValue(TableField<?, T> tableField)
		throws DataAccessException, TooManyRowsException, InvalidResultException {
		return context.fetchOptionalValue(tableField);
	}

	@Override
	public <T> List<T> fetchValues(Table<? extends Record1<T>> table) throws DataAccessException {
		return context.fetchValues(table);
	}

	@Override
	public <T, R extends Record1<T>> List<T> fetchValues(
		ResultQuery<R> resultQuery) throws DataAccessException {
		return context.fetchValues(resultQuery);
	}

	@Override
	public <T> List<T> fetchValues(TableField<?, T> tableField) throws DataAccessException {
		return context.fetchValues(tableField);
	}

	@Override
	public <R extends TableRecord<R>> Result<R> fetchByExample(R r) throws DataAccessException {
		return context.fetchByExample(r);
	}

	@Override
	public int fetchCount(Select<?> select) throws DataAccessException {
		return context.fetchCount(select);
	}

	@Override
	public int fetchCount(Table<?> table) throws DataAccessException {
		return context.fetchCount(table);
	}

	@Override
	public int fetchCount(Table<?> table, Condition condition) throws DataAccessException {
		return context.fetchCount(table, condition);
	}

	@Override
	public int fetchCount(Table<?> table, Condition... conditions) throws DataAccessException {
		return context.fetchCount(table, conditions);
	}

	@Override
	public int fetchCount(Table<?> table,
		Collection<? extends Condition> collection) throws DataAccessException {
		return context.fetchCount(table, collection);
	}

	@Override
	public boolean fetchExists(Select<?> select) throws DataAccessException {
		return context.fetchExists(select);
	}

	@Override
	public boolean fetchExists(Table<?> table) throws DataAccessException {
		return context.fetchExists(table);
	}

	@Override
	public boolean fetchExists(Table<?> table, Condition condition) throws DataAccessException {
		return context.fetchExists(table, condition);
	}

	@Override
	public boolean fetchExists(Table<?> table, Condition... conditions) throws DataAccessException {
		return context.fetchExists(table, conditions);
	}

	@Override
	public boolean fetchExists(Table<?> table,
		Collection<? extends Condition> collection) throws DataAccessException {
		return context.fetchExists(table, collection);
	}

	@Override
	public int execute(Query query) throws DataAccessException {
		return context.execute(query);
	}

	@Override
	@Support
	public <R extends Record> Result<R> fetch(Table<R> table) throws DataAccessException {
		return context.fetch(table);
	}

	@Override
	@Support
	public <R extends Record> Result<R> fetch(Table<R> table,
		Condition condition) throws DataAccessException {
		return context.fetch(table, condition);
	}

	@Override
	@Support
	public <R extends Record> Result<R> fetch(Table<R> table,
		Condition... conditions) throws DataAccessException {
		return context.fetch(table, conditions);
	}

	@Override
	@Support
	public <R extends Record> Result<R> fetch(Table<R> table,
		Collection<? extends Condition> collection) throws DataAccessException {
		return context.fetch(table, collection);
	}

	@Override
	@Support
	public <R extends Record> R fetchOne(Table<R> table)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOne(table);
	}

	@Override
	@Support
	public <R extends Record> R fetchOne(Table<R> table, Condition condition)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOne(table, condition);
	}

	@Override
	@Support
	public <R extends Record> R fetchOne(Table<R> table,
		Condition... conditions) throws DataAccessException, TooManyRowsException {
		return context.fetchOne(table, conditions);
	}

	@Override
	@Support
	public <R extends Record> R fetchOne(Table<R> table,
		Collection<? extends Condition> collection)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOne(table, collection);
	}

	@Override
	@Support
	public <R extends Record> R fetchSingle(Table<R> table)
		throws DataAccessException, NoDataFoundException, TooManyRowsException {
		return context.fetchSingle(table);
	}

	@Override
	@Support
	public <R extends Record> R fetchSingle(Table<R> table,
		Condition condition)
		throws DataAccessException, NoDataFoundException, TooManyRowsException {
		return context.fetchSingle(table, condition);
	}

	@Override
	@Support
	public <R extends Record> R fetchSingle(Table<R> table,
		Condition... conditions)
		throws DataAccessException, NoDataFoundException, TooManyRowsException {
		return context.fetchSingle(table, conditions);
	}

	@Override
	@Support
	public <R extends Record> R fetchSingle(Table<R> table,
		Collection<? extends Condition> collection)
		throws DataAccessException, NoDataFoundException, TooManyRowsException {
		return context.fetchSingle(table, collection);
	}

	@Override
	@Support
	public Record fetchSingle(SelectField<?>... selectFields) throws DataAccessException {
		return context.fetchSingle(selectFields);
	}

	@Override
	@Support
	public Record fetchSingle(
		Collection<? extends SelectField<?>> collection) throws DataAccessException {
		return context.fetchSingle(collection);
	}

	@Override
	@Support
	public <T1> Record1<T1> fetchSingle(SelectField<T1> selectField) throws DataAccessException {
		return context.fetchSingle(selectField);
	}

	@Override
	@Support
	public <T1, T2> Record2<T1, T2> fetchSingle(SelectField<T1> selectField,
		SelectField<T2> selectField1) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1);
	}

	@Override
	@Support
	public <T1, T2, T3> Record3<T1, T2, T3> fetchSingle(SelectField<T1> selectField,
		SelectField<T2> selectField1, SelectField<T3> selectField2) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2);
	}

	@Override
	@Support
	public <T1, T2, T3, T4> Record4<T1, T2, T3, T4> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5> Record5<T1, T2, T3, T4, T5> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6> Record6<T1, T2, T3, T4, T5, T6> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7> Record7<T1, T2, T3, T4, T5, T6, T7> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8> Record8<T1, T2, T3, T4, T5, T6, T7, T8> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9> Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Record11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Record12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Record13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Record14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Record15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Record16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> Record17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> Record18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> Record19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17,
		SelectField<T19> selectField18) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17, selectField18);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> Record20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17,
		SelectField<T19> selectField18, SelectField<T20> selectField19) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17, selectField18, selectField19);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> Record21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17,
		SelectField<T19> selectField18, SelectField<T20> selectField19,
		SelectField<T21> selectField20) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17, selectField18, selectField19, selectField20);
	}

	@Override
	@Support
	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> Record22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> fetchSingle(
		SelectField<T1> selectField, SelectField<T2> selectField1,
		SelectField<T3> selectField2, SelectField<T4> selectField3,
		SelectField<T5> selectField4, SelectField<T6> selectField5,
		SelectField<T7> selectField6, SelectField<T8> selectField7,
		SelectField<T9> selectField8, SelectField<T10> selectField9,
		SelectField<T11> selectField10, SelectField<T12> selectField11,
		SelectField<T13> selectField12, SelectField<T14> selectField13,
		SelectField<T15> selectField14, SelectField<T16> selectField15,
		SelectField<T17> selectField16, SelectField<T18> selectField17,
		SelectField<T19> selectField18, SelectField<T20> selectField19,
		SelectField<T21> selectField20, SelectField<T22> selectField21) throws DataAccessException {
		return context.fetchSingle(selectField, selectField1, selectField2, selectField3,
			selectField4,
			selectField5, selectField6, selectField7, selectField8, selectField9, selectField10,
			selectField11, selectField12, selectField13, selectField14, selectField15,
			selectField16,
			selectField17, selectField18, selectField19, selectField20, selectField21);
	}

	@Override
	@Support
	public <R extends Record> Optional<R> fetchOptional(Table<R> table)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOptional(table);
	}

	@Override
	@Support
	public <R extends Record> Optional<R> fetchOptional(Table<R> table,
		Condition condition) throws DataAccessException, TooManyRowsException {
		return context.fetchOptional(table, condition);
	}

	@Override
	@Support
	public <R extends Record> Optional<R> fetchOptional(Table<R> table,
		Condition... conditions) throws DataAccessException, TooManyRowsException {
		return context.fetchOptional(table, conditions);
	}

	@Override
	@Support
	public <R extends Record> Optional<R> fetchOptional(Table<R> table,
		Collection<? extends Condition> collection)
		throws DataAccessException, TooManyRowsException {
		return context.fetchOptional(table, collection);
	}

	@Override
	@Support
	public <R extends Record> R fetchAny(Table<R> table) throws DataAccessException {
		return context.fetchAny(table);
	}

	@Override
	@Support
	public <R extends Record> R fetchAny(Table<R> table, Condition condition)
		throws DataAccessException {
		return context.fetchAny(table, condition);
	}

	@Override
	@Support
	public <R extends Record> R fetchAny(Table<R> table,
		Condition... conditions) throws DataAccessException {
		return context.fetchAny(table, conditions);
	}

	@Override
	@Support
	public <R extends Record> R fetchAny(Table<R> table,
		Collection<? extends Condition> collection) throws DataAccessException {
		return context.fetchAny(table, collection);
	}

	@Override
	@Support
	public <R extends Record> Cursor<R> fetchLazy(Table<R> table) throws DataAccessException {
		return context.fetchLazy(table);
	}

	@Override
	@Support
	public <R extends Record> Cursor<R> fetchLazy(Table<R> table,
		Condition condition) throws DataAccessException {
		return context.fetchLazy(table, condition);
	}

	@Override
	@Support
	public <R extends Record> Cursor<R> fetchLazy(Table<R> table,
		Condition... conditions) throws DataAccessException {
		return context.fetchLazy(table, conditions);
	}

	@Override
	@Support
	public <R extends Record> Cursor<R> fetchLazy(Table<R> table,
		Collection<? extends Condition> collection) throws DataAccessException {
		return context.fetchLazy(table, collection);
	}

	@Override
	@Support
	public <R extends Record> CompletionStage<Result<R>> fetchAsync(
		Table<R> table) {
		return context.fetchAsync(table);
	}

	@Override
	@Support
	public <R extends Record> CompletionStage<Result<R>> fetchAsync(
		Table<R> table, Condition condition) {
		return context.fetchAsync(table, condition);
	}

	@Override
	@Support
	public <R extends Record> CompletionStage<Result<R>> fetchAsync(
		Table<R> table, Condition... conditions) {
		return context.fetchAsync(table, conditions);
	}

	@Override
	@Support
	public <R extends Record> CompletionStage<Result<R>> fetchAsync(
		Table<R> table, Collection<? extends Condition> collection) {
		return context.fetchAsync(table, collection);
	}

	@Override
	@Support
	public <R extends Record> CompletionStage<Result<R>> fetchAsync(
		Executor executor, Table<R> table) {
		return context.fetchAsync(executor, table);
	}

	@Override
	@Support
	public <R extends Record> CompletionStage<Result<R>> fetchAsync(
		Executor executor, Table<R> table, Condition condition) {
		return context.fetchAsync(executor, table, condition);
	}

	@Override
	@Support
	public <R extends Record> CompletionStage<Result<R>> fetchAsync(
		Executor executor, Table<R> table,
		Condition... conditions) {
		return context.fetchAsync(executor, table, conditions);
	}

	@Override
	@Support
	public <R extends Record> CompletionStage<Result<R>> fetchAsync(
		Executor executor, Table<R> table,
		Collection<? extends Condition> collection) {
		return context.fetchAsync(executor, table, collection);
	}

	@Override
	@Support
	public <R extends Record> Stream<R> fetchStream(Table<R> table) throws DataAccessException {
		return context.fetchStream(table);
	}

	@Override
	@Support
	public <R extends Record> Stream<R> fetchStream(Table<R> table,
		Condition condition) throws DataAccessException {
		return context.fetchStream(table, condition);
	}

	@Override
	@Support
	public <R extends Record> Stream<R> fetchStream(Table<R> table,
		Condition... conditions) throws DataAccessException {
		return context.fetchStream(table, conditions);
	}

	@Override
	@Support
	public <R extends Record> Stream<R> fetchStream(Table<R> table,
		Collection<? extends Condition> collection) throws DataAccessException {
		return context.fetchStream(table, collection);
	}

	@Override
	@Support
	public int executeInsert(TableRecord<?> tableRecord) throws DataAccessException {
		return context.executeInsert(tableRecord);
	}

	@Override
	@Support
	public int executeUpdate(UpdatableRecord<?> updatableRecord) throws DataAccessException {
		return context.executeUpdate(updatableRecord);
	}

	@Override
	@Support
	public int executeUpdate(TableRecord<?> tableRecord, Condition condition)
		throws DataAccessException {
		return context.executeUpdate(tableRecord, condition);
	}

	@Override
	@Support
	public int executeDelete(UpdatableRecord<?> updatableRecord) throws DataAccessException {
		return context.executeDelete(updatableRecord);
	}

	@Override
	@Support
	public int executeDelete(TableRecord<?> tableRecord, Condition condition)
		throws DataAccessException {
		return context.executeDelete(tableRecord, condition);
	}

	@Override
	public Configuration configuration() {
		return context.configuration();
	}

	@Override
	public DSLContext dsl() {
		return context.dsl();
	}

	@Override
	public Settings settings() {
		return context.settings();
	}

	@Override
	public SQLDialect dialect() {
		return context.dialect();
	}

	@Override
	public SQLDialect family() {
		return context.family();
	}

	@Override
	public Map<Object, Object> data() {
		return context.data();
	}

	@Override
	public Object data(Object o) {
		return context.data(o);
	}

	@Override
	public Object data(Object o, Object o1) {
		return context.data(o, o1);
	}
}
