/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

/**
 * Object to represent a SQL parameter definition.
 * Parameters may be anonymous, in which case name is null.
 * However all parameters must define a SQL type constant
 * from java.sql.Types.
 * @author Rod Johnson
 */
public class SqlParameter {

	private String name;
	
	/** SQL type constant from java.sql.Types */
	private int type;

    /** used for types that are user-named like: STRUCT, DISTINCT, JAVA_OBJECT, and named array types. */
	private String typeName;
	
		
	/**
	 * Add a new anonymous parameter
	 */
	public SqlParameter(int type) {
		this(null, type, null);
	}

	public SqlParameter(int type, String typeName) {
		this(null, type, typeName);
	}

	public SqlParameter(String name, int type) {
		this(name, type, null);
	}
	
	public SqlParameter(String name, int type, String typeName) {
		this.name = name;
		this.type = type;
		this.typeName = typeName;
	}

	public String getName() {
		return name;
	}
	
	public int getSqlType() {
		return type;
	}

	public String getTypeName() {
		return typeName;
	}


	/**
	 * Convert a list of JDBC types, as defined in the java.sql.Types class,
	 * to a List of SqlParameter objects as used in this package
	 */
	public static List sqlTypesToAnonymousParameterList(int[] types) {
		List l = new LinkedList();
		if (types != null) {
			for (int i = 0; i < types.length; i++) {
				l.add(new SqlParameter(types[i]));
			}
		}
		return l;
	}

	/**
	 * Implementation of ResultReader that calls the supplied
	 * RowMapper class's mapRow() method for each row.  
	 * This class is used by parameters that return a result set - 
	 * subclasses include SqlOutputParameter and SqlReturnResultSet.  
	 * This class should also be able to be reused when we implement 
	 * functionality to retrieve generated keys for insert statements.
	 */
	class ResultReaderStoredProcImpl implements ResultReader {

		/** List to save results in */
		private List results;

		/** The RowMapper implementation that will be used to map rows */
		private RowMapper rowMapper;

		/** The counter used to count rows */
		private int rowNum = 0;

		/**
		 * Use an array results. More efficient if we know how many results to expect.
		 */
		ResultReaderStoredProcImpl(int rowsExpected, RowMapper rowMapper) {
			// Use the more efficient collection if we know how many rows to expect
			this.results = (rowsExpected > 0) ? (List) new ArrayList(rowsExpected) : (List) new LinkedList();
			this.rowMapper = rowMapper;
		}

		public void processRow(ResultSet rs) throws SQLException {
			results.add(rowMapper.mapRow(rs, rowNum++));
		}

		public List getResults() {
			return results;
		}
	}

}
