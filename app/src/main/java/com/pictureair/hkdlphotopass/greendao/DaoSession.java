package com.pictureair.hkdlphotopass.greendao;

import java.util.Map;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import com.pictureair.hkdlphotopass.entity.ADLocationInfo;
import com.pictureair.hkdlphotopass.entity.FirstStartInfo;
import com.pictureair.hkdlphotopass.entity.FrameOrStikerInfo;
import com.pictureair.hkdlphotopass.entity.JsonInfo;
import com.pictureair.hkdlphotopass.entity.PaymentOrderInfo;
import com.pictureair.hkdlphotopass.entity.PhotoDownLoadInfo;
import com.pictureair.hkdlphotopass.entity.PhotoInfo;
import com.pictureair.hkdlphotopass.entity.ThreadInfo;

import com.pictureair.hkdlphotopass.greendao.ADLocationInfoDao;
import com.pictureair.hkdlphotopass.greendao.FirstStartInfoDao;
import com.pictureair.hkdlphotopass.greendao.FrameOrStikerInfoDao;
import com.pictureair.hkdlphotopass.greendao.JsonInfoDao;
import com.pictureair.hkdlphotopass.greendao.PaymentOrderInfoDao;
import com.pictureair.hkdlphotopass.greendao.PhotoDownLoadInfoDao;
import com.pictureair.hkdlphotopass.greendao.PhotoInfoDao;
import com.pictureair.hkdlphotopass.greendao.ThreadInfoDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see org.greenrobot.greendao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig aDLocationInfoDaoConfig;
    private final DaoConfig firstStartInfoDaoConfig;
    private final DaoConfig frameOrStikerInfoDaoConfig;
    private final DaoConfig jsonInfoDaoConfig;
    private final DaoConfig paymentOrderInfoDaoConfig;
    private final DaoConfig photoDownLoadInfoDaoConfig;
    private final DaoConfig photoInfoDaoConfig;
    private final DaoConfig threadInfoDaoConfig;

    private final ADLocationInfoDao aDLocationInfoDao;
    private final FirstStartInfoDao firstStartInfoDao;
    private final FrameOrStikerInfoDao frameOrStikerInfoDao;
    private final JsonInfoDao jsonInfoDao;
    private final PaymentOrderInfoDao paymentOrderInfoDao;
    private final PhotoDownLoadInfoDao photoDownLoadInfoDao;
    private final PhotoInfoDao photoInfoDao;
    private final ThreadInfoDao threadInfoDao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        aDLocationInfoDaoConfig = daoConfigMap.get(ADLocationInfoDao.class).clone();
        aDLocationInfoDaoConfig.initIdentityScope(type);

        firstStartInfoDaoConfig = daoConfigMap.get(FirstStartInfoDao.class).clone();
        firstStartInfoDaoConfig.initIdentityScope(type);

        frameOrStikerInfoDaoConfig = daoConfigMap.get(FrameOrStikerInfoDao.class).clone();
        frameOrStikerInfoDaoConfig.initIdentityScope(type);

        jsonInfoDaoConfig = daoConfigMap.get(JsonInfoDao.class).clone();
        jsonInfoDaoConfig.initIdentityScope(type);

        paymentOrderInfoDaoConfig = daoConfigMap.get(PaymentOrderInfoDao.class).clone();
        paymentOrderInfoDaoConfig.initIdentityScope(type);

        photoDownLoadInfoDaoConfig = daoConfigMap.get(PhotoDownLoadInfoDao.class).clone();
        photoDownLoadInfoDaoConfig.initIdentityScope(type);

        photoInfoDaoConfig = daoConfigMap.get(PhotoInfoDao.class).clone();
        photoInfoDaoConfig.initIdentityScope(type);

        threadInfoDaoConfig = daoConfigMap.get(ThreadInfoDao.class).clone();
        threadInfoDaoConfig.initIdentityScope(type);

        aDLocationInfoDao = new ADLocationInfoDao(aDLocationInfoDaoConfig, this);
        firstStartInfoDao = new FirstStartInfoDao(firstStartInfoDaoConfig, this);
        frameOrStikerInfoDao = new FrameOrStikerInfoDao(frameOrStikerInfoDaoConfig, this);
        jsonInfoDao = new JsonInfoDao(jsonInfoDaoConfig, this);
        paymentOrderInfoDao = new PaymentOrderInfoDao(paymentOrderInfoDaoConfig, this);
        photoDownLoadInfoDao = new PhotoDownLoadInfoDao(photoDownLoadInfoDaoConfig, this);
        photoInfoDao = new PhotoInfoDao(photoInfoDaoConfig, this);
        threadInfoDao = new ThreadInfoDao(threadInfoDaoConfig, this);

        registerDao(ADLocationInfo.class, aDLocationInfoDao);
        registerDao(FirstStartInfo.class, firstStartInfoDao);
        registerDao(FrameOrStikerInfo.class, frameOrStikerInfoDao);
        registerDao(JsonInfo.class, jsonInfoDao);
        registerDao(PaymentOrderInfo.class, paymentOrderInfoDao);
        registerDao(PhotoDownLoadInfo.class, photoDownLoadInfoDao);
        registerDao(PhotoInfo.class, photoInfoDao);
        registerDao(ThreadInfo.class, threadInfoDao);
    }
    
    public void clear() {
        aDLocationInfoDaoConfig.clearIdentityScope();
        firstStartInfoDaoConfig.clearIdentityScope();
        frameOrStikerInfoDaoConfig.clearIdentityScope();
        jsonInfoDaoConfig.clearIdentityScope();
        paymentOrderInfoDaoConfig.clearIdentityScope();
        photoDownLoadInfoDaoConfig.clearIdentityScope();
        photoInfoDaoConfig.clearIdentityScope();
        threadInfoDaoConfig.clearIdentityScope();
    }

    public ADLocationInfoDao getADLocationInfoDao() {
        return aDLocationInfoDao;
    }

    public FirstStartInfoDao getFirstStartInfoDao() {
        return firstStartInfoDao;
    }

    public FrameOrStikerInfoDao getFrameOrStikerInfoDao() {
        return frameOrStikerInfoDao;
    }

    public JsonInfoDao getJsonInfoDao() {
        return jsonInfoDao;
    }

    public PaymentOrderInfoDao getPaymentOrderInfoDao() {
        return paymentOrderInfoDao;
    }

    public PhotoDownLoadInfoDao getPhotoDownLoadInfoDao() {
        return photoDownLoadInfoDao;
    }

    public PhotoInfoDao getPhotoInfoDao() {
        return photoInfoDao;
    }

    public ThreadInfoDao getThreadInfoDao() {
        return threadInfoDao;
    }

}
