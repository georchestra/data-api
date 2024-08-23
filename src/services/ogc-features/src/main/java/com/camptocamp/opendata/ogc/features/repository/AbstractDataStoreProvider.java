package com.camptocamp.opendata.ogc.features.repository;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.geotools.api.data.DataStore;
import org.springframework.beans.factory.DisposableBean;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractDataStoreProvider implements DataStoreProvider, DisposableBean {

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

    protected abstract @NonNull DataStore create();

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
