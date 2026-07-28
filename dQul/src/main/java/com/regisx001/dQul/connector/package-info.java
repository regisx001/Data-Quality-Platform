/**
 * Connector package for datasource abstraction.
 *
 * <p>
 * This layer hides the implementation details of PostgreSQL, CSV, and future
 * data sources behind a single, consistent interface ({@link
 * com.regisx001.dQul.connector.api.DataSourceConnector}). Every other component
 * of the platform communicates only with this abstraction.
 *
 * <p>
 * Each connector is responsible for:
 * <ul>
 * <li><b>Connection</b> &mdash; establishing connectivity and
 * authentication</li>
 * <li><b>Discovery</b> &mdash; enumerating available datasets</li>
 * <li><b>Metadata</b> &mdash; extracting schema information</li>
 * <li><b>Reading</b> &mdash; providing a Spark-compatible data reader</li>
 * </ul>
 *
 * <p>
 * The {@link com.regisx001.dQul.connector.ConnectorFactory} is the central
 * entry point. It accepts a
 * {@link com.regisx001.dQul.connector.api.ConnectorConfig}
 * and returns the appropriate connector implementation.
 *
 * @see com.regisx001.dQul.connector.api.DataSourceConnector
 * @see com.regisx001.dQul.connector.ConnectorFactory
 */
package com.regisx001.dQul.connector;
