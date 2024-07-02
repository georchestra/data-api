package com.camptocamp.opendata.ogc.features.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.springframework.beans.factory.DisposableBean;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultDataStoreProvider implements DataStoreProvider, DisposableBean {

    protected final @NonNull Map<String, Object> connectionParams;

    protected final ReadWriteLock lock = new ReentrantReadWriteLock();

    protected DataStore store;

    @Override
    public DataStore get() {
        DataStore ds = this.store;
        lock.readLock().lock();
        if (ds == null) {
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                ds = this.store;
                if (ds == null) {
                    ds = create();
                    this.store = ds;
                }
                lock.readLock().lock();// downgrade
            } finally {
                lock.writeLock().unlock();
            }
        }
        try {
            return ds;
        } finally {
            lock.readLock().unlock();
        }
    }

    protected @NonNull DataStore create() {
        try {
            return Objects.requireNonNull(DataStoreFinder.getDataStore(connectionParams),
                    "Unable to find datastore with the provided connection parameters");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void reInit() {
        lock.writeLock().lock();
        try {
            destroy();
            store = create();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void destroy() {
        lock.writeLock().lock();
        try {
            if (store != null) {
                store.dispose();
                store = null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

}
