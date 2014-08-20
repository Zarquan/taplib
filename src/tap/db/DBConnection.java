package tap.db;

/*
 * This file is part of TAPLibrary.
 * 
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import tap.TAPFactory;
import tap.data.DataReadException;
import tap.data.TableIterator;
import tap.metadata.TAPColumn;
import tap.metadata.TAPMetadata;
import tap.metadata.TAPTable;
import uws.service.log.UWSLogType;
import adql.query.ADQLQuery;
import adql.translator.ADQLTranslator;

/**
 * <p>Connection to the "database" (whatever is the type or whether it is linked to a true DBMS connection).</p>
 * 
 * <p>It lets executing ADQL queries and updating the TAP datamodel (with the list of schemas, tables and columns published in TAP,
 * or with uploaded tables).</p>
 * 
 * <p><b>IMPORTANT:</b>
 * 	This connection aims only to provide a common and known interface for any kind of database connection.
 * 	A connection MUST be opened/created and closed/freed ONLY by the {@link TAPFactory}, which will usually merely wrap
 * 	the real database connection by a {@link DBConnection} object. That's why this interface does not provide anymore
 * 	a close() function.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2014)
 */
public interface DBConnection {

	/** Log type specific to the database activity.
	 * @see UWSLogType#createCustomLogType(String) */
	public final static UWSLogType LOG_TYPE_DB_ACTIVITY = UWSLogType.createCustomLogType("DBActivity");

	/**
	 * <p>Get any identifier for this connection.</p>
	 * 
	 * <p><i>note: it is used only for logging purpose.</i></p>
	 * 
	 * @return	ID of this connection.
	 */
	public String getID();

	/**
	 * <p>Fetch the whole content of TAP_SCHEMA.</p>
	 * 
	 * <p>
	 * 	This function SHOULD be used only once: at the starting of the TAP service. It is an alternative way
	 * 	to get the published schemas, tables and columns. The other way is to build a {@link TAPMetadata} object
	 * 	yourself in function of the schemas/tables/columns you want to publish (i.e. which can be done by reading
	 * 	metadata from a XML document - following the same schema - XSD- as for the TAP resource <i>tables</i>)
	 * 	and then to load them in the DB (see {@link #setTAPSchema(TAPMetadata, boolean)} for more details).
	 * </p>
	 * 
	 * <p><b>CAUTION:
	 * 	This function MUST NOT be used if the tables to publish or the standard TAP_SCHEMA tables have names in DB different from the
	 * 	ones defined by the TAP standard. So, if DB names are different from the ADQL names, you have to write yourself a way to get
	 * 	the metadata from the DB.
	 * </b></p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	If the schema or some standard tables or columns are missing, TAP_SCHEMA will be considered as incomplete
	 * 	and an exception will be thrown.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	This function MUST be able to read the standard tables and columns described by the IVOA. All other tables/columns
	 * 	will be merely ignored.
	 * </i></p>
	 * 
	 * @return	Content of TAP_SCHEMA inside the DB.
	 * 
	 * @throws DBException	If TAP_SCHEMA can not be found, is incomplete or if some important metadata can not be retrieved.
	 * 
	 * @since 2.0
	 */
	public TAPMetadata getTAPSchema() throws DBException;

	/**
	 * <p>Empty and then fill all the TAP_SCHEMA tables with the given list of metadata.</p>
	 * 
	 * <p>
	 * 	This function SHOULD be used only once: at the starting of the TAP service,
	 * 	when metadata are loaded from a XML document (following the same schema - XSD-
	 * 	as for the TAP resource <i>tables</i>).
	 * </p>
	 * 
	 * <p>
	 * 	<i>THIS FUNCTION IS MANIPULATING THE SCHEMAS AND TABLES OF YOUR DATABASE.
	 * 	SO IT SHOULD HAVE A SPECIFIC BEHAVIOR DESCRIBED BELOW.
	 * 	<b>SO PLEASE READ THE FOLLOWINGS AND TRY TO RESPECT IT AS MUCH AS POSSIBLE IN THE IMPLEMENTATIONS</b>
	 * </i></p>
	 * 
	 * <h3>TAP_SCHEMA CREATION</h3>
	 * <p>
	 * 	This function is MAY drop and then re-create the schema TAP_SCHEMA and all
	 * 	its tables listed in the TAP standard (TAP_SCHEMA.schemas, .tables, .columns, .keys and .key_columns).
	 * 	<i>All other tables inside TAP_SCHEMA SHOULD NOT be modified!</i>
	 * </p>
	 * 
	 * <p>
	 * 	The schema and the tables MUST be created using either the <b>standard definition</b> or the
	 * 	<b>definition provided in the {@link TAPMetadata} object</b> (if provided). Indeed, if your definition of these TAP tables
	 * 	is different from the standard (the standard + new elements), you MUST provide your modifications in parameter
	 *	through the {@link TAPMetadata} object so that they can be applied and taken into account in TAP_SCHEMA.
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	DB names provided in the given TAPMetadata (see {@link TAPTable#getDBSchemaName()}, {@link TAPTable#getDBName()} and {@link TAPColumn#getDBName()})
	 * 	are used for the creation and filling of the tables.
	 * 
	 * 	Whether these requests must be case sensitive or not SHOULD be managed by ADQLTranslator (see {@link ADQLTranslator#getQualifiedSchemaName(adql.db.DBTable)},
	 * {@link ADQLTranslator#getQualifiedTableName(adql.db.DBTable)} and {@link ADQLTranslator#getColumnName(adql.db.DBColumn)}).
	 * </i></p>
	 * 
	 * <h3>TAPMetadata PARAMETER</h3>
	 * <p>
	 * 	This object MUST contain all schemas, tables and columns that MUST be published. All its content will be
	 * 	used in order to fill the TAP_SCHEMA tables.
	 * </p>
	 * <p>
	 * 	Of course, TAP_SCHEMA and its tables MAY be provided in this object. But:
	 * </p>
	 * <ul>
	 * 		<li><b>(a) if TAP_SCHEMA tables are NOT provided</b>:
	 * 			this function SHOULD consider their definition as exactly the one provided by
	 * 			the TAP standard/protocol. If so, the standard definition MUST be automatically added
	 * 			into the {@link TAPMetadata} object AND into TAP_SCHEMA. 
	 * 		</li>
	 * 		<li><b>(b) if TAP_SCHEMA tables ARE provided</b>:
	 * 			the definition of all given elements will be taken into account while updating the TAP_SCHEMA.
	 * 			Each element definition not provided MUST be considered as exactly the same as the standard one
	 * 			and MUST be added into the {@link TAPMetadata} object AND into TAP_SCHEMA.
	 *		</li>
	 * </ul>
	 * 
	 * <p><i>Note: By default, all implementations of this interface in the TAP library will fill only standard columns and tables of TAP_SCHEMA.
	 * To fill your own, you MUST implement yourself this interface or to extend an existing implementation.</i></p>
	 * 
	 * <p><i><b>WARNING</b>:
	 * 	(b) lets consider a TAP_SCHEMA different from the standard one. BUT, these differences MUST be only additions,
	 * 	NOT modifications or deletion of the standard definition! This function MUST be able to work AT LEAST on a
	 * 	standard definition of TAP_SCHEMA.
	 * </p>
	 * 
	 * <h3>FILLING BEHAVIOUR</h3>
	 * <p>
	 * 	The TAP_SCHEMA tables SHOULD be completely emptied (in SQL: "DELETE FROM &lt;table_name&gt;;" or merely "DROP TABLE &lt;table_name&gt;") before insertions can be processed.
	 * </p>
	 * 
	 * <h3>ERRORS MANAGEMENT</h3>
	 * <p>
	 * 	If any error occurs while executing any "DB" queries (in SQL: DROP, DELETE, INSERT, CREATE, ...), all queries executed
	 * 	before in this function MUST be canceled (in SQL: ROLLBACK).
	 * </p>
	 * 
	 * @param metadata			List of all schemas, tables, columns and foreign keys to insert in the TAP_SCHEMA.
	 * 
	 * @throws DBException			If any error occurs while updating the database.
	 * 
	 * @since 2.0
	 */
	public void setTAPSchema(final TAPMetadata metadata) throws DBException;

	/**
	 * Add the defined and given table inside the TAP_UPLOAD schema.
	 * 
	 * <p><i>note: A table of TAP_UPLOAD MUST be transient and persistent only for the lifetime of the query.
	 * So, this function should always be used with {@link #dropUploadedTable(String)}, which is called at
	 * the end of each query execution.</i></p> 
	 * 
	 * @param tableDef	Definition of the table to upload (list of all columns and of their type).
	 * @param data		Rows and columns of the table to upload.
	 * 
	 * @return			<i>true</i> if the given table has been successfully added, <i>false</i> otherwise.
	 * 
	 * @throws DBException			If any error occurs while adding the table.
	 * @throws DataReadException	If any error occurs while reading the given data (particularly if any limit - in byte or row - set in the TableIterator is reached).
	 * 
	 * @since 2.0
	 */
	public boolean addUploadedTable(final TAPTable tableDef, final TableIterator data) throws DBException, DataReadException;

	/**
	 * <p>Drop the specified uploaded table from the database.
	 * More precisely, it means dropping a table from the TAP_UPLOAD schema.</p>
	 * 
	 * <p><i>Note:
	 * 	This function SHOULD drop only one table. So, if more than one table match in the "database" to the given one, an exception MAY be thrown.
	 * 	This behavior is implementation-dependent.
	 * </i></p>
	 * 
	 * @param tableDef	Definition of the uploaded table to drop (the whole object is needed in order to get the DB schema and tables names).
	 * 
	 * @return	<i>true</i> if the specified table has been successfully dropped, <i>false</i> otherwise.
	 * 
	 * @throws DBException	If any error occurs while dropping the specified uploaded table.
	 * 
	 * @since 2.0
	 */
	public boolean dropUploadedTable(final TAPTable tableDef) throws DBException;

	/**
	 * <p>Let executing the given ADQL query.</p>
	 * 
	 * <p>The result of this query must be formatted as a table, and so must be iterable using a {@link TableIterator}.</p>
	 * 
	 * <p><i>note: the interpretation of the ADQL query is up to the implementation. In most of the case, it is just needed
	 * to translate this ADQL query into an SQL query (understandable by the chosen DBMS).</i></p>
	 * 
	 * @param adqlQuery	ADQL query to execute.
	 * 
	 * @return	The table result.
	 * 
	 * @throws DBException	If any errors occurs while executing the query.
	 * 
	 * @since 2.0
	 */
	public TableIterator executeQuery(final ADQLQuery adqlQuery) throws DBException;

}
