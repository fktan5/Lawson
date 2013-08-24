package net.fktan5.lawson;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class AkikoProvider extends ContentProvider {

    static final GetTableAndWhereOutParameter sGetTableAndWhereParam = new GetTableAndWhereOutParameter();
    public static final String AUTHORITY = "hackalawson.app.akikoprovider";
    private static final UriMatcher sUriMatcher;
    AkikoDatabaseHelper mHelper;

    @Override
    public boolean onCreate() {
        mHelper = new AkikoDatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case TypeCodes.AKIKO_DATA:
                return Entities.AkikoTable.CONTENT_TYPE;
            case TypeCodes.AKIKO_DATA_ID:
                return Entities.AkikoTable.CONTENT_ITEM_TYPE;
            case TypeCodes.AKIKO_TWEET_DATA:
                return Entities.AkikoTweetTable.CONTENT_TYPE;
            case TypeCodes.AKIKO_TWEET_DATA_ID:
                return Entities.AkikoTweetTable.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projectionIn, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }

        final List<String> prependArgs = new ArrayList<String>();
        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        final String limit = uri.getQueryParameter("limit");
        String sort = sortOrder;
        String groupBy = null;

        if (uri.getQueryParameter("distinct") != null) {
            qb.setDistinct(true);
        }

        switch (sUriMatcher.match(uri)) {
            case TypeCodes.AKIKO_DATA:
                qb.setTables(Entities.AkikoTable.TABLE_NAME);
                sort = TextUtils.isEmpty(sort) ? Entities.AkikoTable.DEFAULT_SORT_ORDER : sort;
                break;
            case TypeCodes.AKIKO_DATA_ID:
                qb.setTables(Entities.AkikoTable.TABLE_NAME);
                qb.appendWhere("_id=?");
                prependArgs.add(uri.getLastPathSegment());
                break;
            case TypeCodes.AKIKO_TWEET_DATA:
                qb.setTables(Entities.AkikoTweetTable.TABLE_NAME);
                sort = TextUtils.isEmpty(sort) ? Entities.AkikoTweetTable.DEFAULT_SORT_ORDER : sort;
                break;
            case TypeCodes.AKIKO_TWEET_DATA_ID:
                qb.setTables(Entities.AkikoTweetTable.TABLE_NAME);
                qb.appendWhere("_id=?");
                prependArgs.add(uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        final Cursor c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        synchronized (AkikoDatabaseHelper.sDataLock) {
            final int match = sUriMatcher.match(uri);

            ArrayList<Long> notifyRowIds = new ArrayList<Long>();
            Uri newUri = insertInternal(uri, match, initialValues, notifyRowIds);

            if (newUri != null) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
            return newUri;
        }
    }

    private Uri insertInternal(Uri uri, int match, ContentValues initialValues, ArrayList<Long> notifyRowIds) {
        final long rowId;
        final Uri newUri;

        final SQLiteDatabase db = mHelper.getWritableDatabase();
        if (db == null) {
            return null;
        }

        switch (match) {
            case TypeCodes.AKIKO_DATA:
                rowId = db.insert(Entities.AkikoTable.TABLE_NAME, null, initialValues);
                newUri = (rowId <= 0) ? null : ContentUris.withAppendedId(uri, rowId);
                break;

            case TypeCodes.AKIKO_TWEET_DATA:
                rowId = db.insert(Entities.AkikoTweetTable.TABLE_NAME, null, initialValues);
                newUri = (rowId <= 0) ? null : ContentUris.withAppendedId(uri, rowId);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        return newUri;
    }

    @Override
    public int delete(Uri uri, String userWhere, String[] whereArgs) {
        synchronized (AkikoDatabaseHelper.sDataLock) {
            int count;
            int match = sUriMatcher.match(uri);

            SQLiteDatabase db = mHelper.getWritableDatabase();

            synchronized (sGetTableAndWhereParam) {
                getTableAndWhere(uri, match, userWhere, sGetTableAndWhereParam);
                switch (match) {
                    default:
                        count = db.delete(sGetTableAndWhereParam.table, sGetTableAndWhereParam.where, whereArgs);
                        break;
                }

                Uri notifyUri = Uri.parse("content://" + AUTHORITY + "/");
                getContext().getContentResolver().notifyChange(notifyUri, null);
            }

            return count;
        }
    }

    @Override
    public int update(Uri uri, ContentValues initialValues, String userWhere, String[] whereArgs) {
        synchronized (AkikoDatabaseHelper.sDataLock) {
            final int count;

            final SQLiteDatabase db = mHelper.getWritableDatabase();
            if (db == null) {
                return 0;
            }

            final int match = sUriMatcher.match(uri);

            synchronized (sGetTableAndWhereParam) {
                getTableAndWhere(uri, match, userWhere, sGetTableAndWhereParam);

                switch (match) {
                    case TypeCodes.AKIKO_DATA:
                    case TypeCodes.AKIKO_DATA_ID:
                    case TypeCodes.AKIKO_TWEET_DATA:
                    case TypeCodes.AKIKO_TWEET_DATA_ID:
                        ContentValues values = new ContentValues(initialValues);
                        count = db.update(sGetTableAndWhereParam.table, values, sGetTableAndWhereParam.where, whereArgs);
                        break;

                    default:
                        count = db.update(sGetTableAndWhereParam.table, initialValues, sGetTableAndWhereParam.where, whereArgs);
                        break;
                }
            }

            // Taken from com/android/providers/media/MediaProvider.java
            // in a transaction, the code that began the transaction should be taking
            // care of notifications once it ends the transaction successfully
            if (count > 0 && !db.inTransaction()) {
                getContext().getContentResolver().notifyChange(uri, null);
            }

            return count;
        }
    }

    private String[] combine(List<String> prepend, String[] userArgs) {
        int presize = prepend.size();
        if (presize == 0) {
            return userArgs;
        }

        int usersize = (userArgs != null) ? userArgs.length : 0;
        String[] combined = new String[presize + usersize];
        for (int i = 0; i < presize; i++) {
            combined[i] = prepend.get(i);
        }
        for (int i = 0; i < usersize; i++) {
            combined[presize + i] = userArgs[i];
        }
        return combined;
    }

    private void getTableAndWhere(Uri uri, int match, String userWhere, GetTableAndWhereOutParameter out) {
        String where = null;
        switch (match) {
            case TypeCodes.AKIKO_DATA:
                out.table = Entities.AkikoTable.TABLE_NAME;
                break;

            case TypeCodes.AKIKO_DATA_ID:
                out.table = Entities.AkikoTable.TABLE_NAME;
                where = "_id = " + uri.getLastPathSegment();
                break;

            case TypeCodes.AKIKO_TWEET_DATA:
                out.table = Entities.AkikoTweetTable.TABLE_NAME;
                break;

            case TypeCodes.AKIKO_TWEET_DATA_ID:
                out.table = Entities.AkikoTweetTable.TABLE_NAME;
                where = "_id = " + uri.getLastPathSegment();
                break;

            default:
                throw new UnsupportedOperationException("Unknown or unsupported URL: " + uri.toString());
        }

        if (!TextUtils.isEmpty(userWhere)) {
            if (!TextUtils.isEmpty(where)) {
                out.where = where + " AND (" + userWhere + ")";
            } else {
                out.where = userWhere;
            }
        } else {
            out.where = where;
        }
    }

    interface TypeCodes {
        public static final int AKIKO_DATA = 100;
        public static final int AKIKO_DATA_ID = 101;
        public static final int AKIKO_TWEET_DATA = 102;
        public static final int AKIKO_TWEET_DATA_ID = 103;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, Entities.AkikoTable.PATH, TypeCodes.AKIKO_DATA);
        sUriMatcher.addURI(AUTHORITY, Entities.AkikoTable.PATH + "/#", TypeCodes.AKIKO_DATA_ID);
        sUriMatcher.addURI(AUTHORITY, Entities.AkikoTweetTable.PATH, TypeCodes.AKIKO_TWEET_DATA);
        sUriMatcher.addURI(AUTHORITY, Entities.AkikoTweetTable.PATH + "/#", TypeCodes.AKIKO_TWEET_DATA_ID);
    }

    private static final class GetTableAndWhereOutParameter {
        public String table;
        public String where;
    }

    private static final class Entities {
        public static interface AkikoTable extends BaseColumns {
            public static final String TABLE_NAME = "Akiko";
            static final String PATH = "akiko";
            public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH);
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AkikoProvider.class.getPackage().getName() + PATH;
            public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AkikoProvider.class.getPackage().getName() + PATH;
            /**
             * デフォルトのソート条件です。
             */
            public static final String DEFAULT_SORT_ORDER = _ID + " ASC";

            // カラム名定義
            /**
             * 店舗IDカラム
             */
            public static final String COLUMN_STORE_ID = "storeId";
            /**
             * 押しレベルカラム
             */
            public static final String COLUMN_LEVEL = "level";

        }

        public static interface AkikoTweetTable extends BaseColumns {
            public static final String TABLE_NAME = "AkikoTweet";
            static final String PATH = "akiko_tweet";
            public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH);
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AkikoProvider.class.getPackage().getName() + PATH;
            public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AkikoProvider.class.getPackage().getName() + PATH;
            /**
             * デフォルトのソート条件です。
             */
            public static final String DEFAULT_SORT_ORDER = _ID + " ASC";

            // カラム名定義
            /**
             * tweet文字列カラム
             */
            public static final String COLUMN_TWEET = "tweet";
        }
    }

    private static class AkikoDatabaseHelper extends SQLiteOpenHelper {

        /**
         * バックアップ/リストア用の同期用オブジェクト
         */
        public static final Object[] sDataLock = new Object[0];
        private static final int DATABASE_VERSION = 1;
        private static final String DATABASE_NAME = "akiko.db";
        private static final String[] CREATE_TABLE_DDL;
        private static final String[] DROP_TABLE_DDL;

        static {
            CREATE_TABLE_DDL = new String[]{
                    "CREATE TABLE Akiko ("
                            + BaseColumns._ID + " INTEGER PRIMARY KEY,"
                            + "storeId INTEGER,"
                            + "level INTEGER"
                            + ");",
                    "CREATE TABLE AkikoTweet ("
                            + BaseColumns._ID + " INTEGER PRIMARY KEY,"
                            + "tweet TEXT"
                            + ");",
            };

            DROP_TABLE_DDL = new String[]{
                    "DROP TABLE IF EXISTS Akiko;",
                    "DROP TABLE IF EXISTS AkikoTweet;",
            };
        }

        AkikoDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        private static final void withinTransaction(SQLiteDatabase db, String[] scripts) {
            db.beginTransaction();
            for (String each : scripts) {
                if (TextUtils.isEmpty(each)) {
                    continue;
                }
                db.execSQL(each);
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            withinTransaction(db, CREATE_TABLE_DDL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            withinTransaction(db, DROP_TABLE_DDL);
            onCreate(db);
        }

    }

}