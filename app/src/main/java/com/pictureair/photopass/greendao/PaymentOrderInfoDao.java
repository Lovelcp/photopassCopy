package com.pictureair.photopass.greendao;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import com.pictureair.photopass.entity.PaymentOrderInfo;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "PAYMENT_ORDER_INFO".
*/
public class PaymentOrderInfoDao extends AbstractDao<PaymentOrderInfo, Long> {

    public static final String TABLENAME = "PAYMENT_ORDER_INFO";

    /**
     * Properties of entity PaymentOrderInfo.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property OrderId = new Property(1, String.class, "orderId", false, "ORDER_ID");
        public final static Property UserId = new Property(2, String.class, "userId", false, "USER_ID");
    }


    public PaymentOrderInfoDao(DaoConfig config) {
        super(config);
    }
    
    public PaymentOrderInfoDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"PAYMENT_ORDER_INFO\" (" + //
                "\"_id\" INTEGER PRIMARY KEY ," + // 0: id
                "\"ORDER_ID\" TEXT," + // 1: orderId
                "\"USER_ID\" TEXT);"); // 2: userId
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"PAYMENT_ORDER_INFO\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, PaymentOrderInfo entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        String orderId = entity.getOrderId();
        if (orderId != null) {
            stmt.bindString(2, orderId);
        }
 
        String userId = entity.getUserId();
        if (userId != null) {
            stmt.bindString(3, userId);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, PaymentOrderInfo entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        String orderId = entity.getOrderId();
        if (orderId != null) {
            stmt.bindString(2, orderId);
        }
 
        String userId = entity.getUserId();
        if (userId != null) {
            stmt.bindString(3, userId);
        }
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public PaymentOrderInfo readEntity(Cursor cursor, int offset) {
        PaymentOrderInfo entity = new PaymentOrderInfo( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1), // orderId
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2) // userId
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, PaymentOrderInfo entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setOrderId(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setUserId(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(PaymentOrderInfo entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(PaymentOrderInfo entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(PaymentOrderInfo entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}