package com.pictureair.photopass.greendao;

import java.util.Map;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import com.pictureair.photopass.entity.PhotoInfo2;

import com.pictureair.photopass.greendao.PhotoInfo2Dao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see org.greenrobot.greendao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig photoInfo2DaoConfig;

    private final PhotoInfo2Dao photoInfo2Dao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        photoInfo2DaoConfig = daoConfigMap.get(PhotoInfo2Dao.class).clone();
        photoInfo2DaoConfig.initIdentityScope(type);

        photoInfo2Dao = new PhotoInfo2Dao(photoInfo2DaoConfig, this);

        registerDao(PhotoInfo2.class, photoInfo2Dao);
    }
    
    public void clear() {
        photoInfo2DaoConfig.clearIdentityScope();
    }

    public PhotoInfo2Dao getPhotoInfo2Dao() {
        return photoInfo2Dao;
    }

}
