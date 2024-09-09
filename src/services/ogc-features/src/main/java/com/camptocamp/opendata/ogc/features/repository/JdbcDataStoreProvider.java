package com.camptocamp.opendata.ogc.features.repository;

import javax.sql.DataSource;

public interface JdbcDataStoreProvider extends DataStoreProvider {

    public DataSource getDataSource();

    public void setDataSource(DataSource ds);
}