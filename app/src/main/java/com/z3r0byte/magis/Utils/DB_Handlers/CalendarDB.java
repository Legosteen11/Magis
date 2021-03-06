/*
 * Copyright 2016 Bas van den Boom 'Z3r0byte'
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.z3r0byte.magis.Utils.DB_Handlers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.Gson;
import com.z3r0byte.magis.Utils.DateUtils;

import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.sub.Classroom;
import net.ilexiconn.magister.container.sub.Link;
import net.ilexiconn.magister.container.sub.SubSubject;
import net.ilexiconn.magister.container.sub.Teacher;
import net.ilexiconn.magister.container.type.AppointmentType;
import net.ilexiconn.magister.container.type.InfoType;
import net.ilexiconn.magister.util.DateUtil;

import java.text.ParseException;
import java.util.Date;

import static com.z3r0byte.magis.Utils.DateUtils.addDays;
import static com.z3r0byte.magis.Utils.DateUtils.addMinutes;
import static com.z3r0byte.magis.Utils.DateUtils.formatDate;
import static com.z3r0byte.magis.Utils.DateUtils.getToday;
import static java.lang.Integer.parseInt;


public class CalendarDB extends SQLiteOpenHelper {

    private static final String TAG = "CalendarDB";

    private static final int DATABASE_VERSION = 12;

    private static final String DATABASE_NAME = "calendarDB";
    private static final String TABLE_CALENDAR = "calendar";

    private static final String KEY_ID = "id";
    private static final String KEY_DESC = "description";
    private static final String KEY_CALENDAR_ID = "calendarId";
    private static final String KEY_CLASS_ROOMS = "classrooms";
    private static final String KEY_TEACHER = "teachers";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_START = "start";
    private static final String KEY_END = "end";
    private static final String KEY_FORMATTED_END = "formatend";
    private static final String KEY_FORMATTED_START = "formatstart";
    private static final String KEY_FORMATTED_END_2 = "formatend2";
    private static final String KEY_FORMATTED_START_2 = "formatstart2";
    private static final String KEY_PERIOD_FROM = "periodFrom";
    private static final String KEY_PERIOD_TO = "periodTo";
    private static final String KEY_TAKES_ALL_DAY = "takesAllDay";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_STATE = "state";
    private static final String KEY_FINISHED = "finished";
    private static final String KEY_LINKS = "links";
    private static final String KEY_TYPE = "type";
    private static final String KEY_INFO_TYPE = "infoType";
    private static final String KEY_SUBJECTS = "subjects";

    public CalendarDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CALENDAR_TABLE = "CREATE TABLE IF NOT EXISTS "
                + TABLE_CALENDAR + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_DESC + " TEXT,"
                + KEY_CALENDAR_ID + " INTEGER,"
                + KEY_CLASS_ROOMS + " TEXT,"
                + KEY_CONTENT + " TEXT,"
                + KEY_END + " TEXT,"
                + KEY_FINISHED + " BOOLEAN,"
                + KEY_FORMATTED_END + " INTEGER,"
                + KEY_FORMATTED_START + " INTEGER,"
                + KEY_FORMATTED_END_2 + " INTEGER,"
                + KEY_FORMATTED_START_2 + " INTEGER,"
                + KEY_INFO_TYPE + " TEXT,"
                + KEY_LINKS + " TEXT,"
                + KEY_LOCATION + " TEXT,"
                + KEY_PERIOD_FROM + " INTEGER,"
                + KEY_PERIOD_TO + " INTEGER,"
                + KEY_SUBJECTS + " TEXT,"
                + KEY_START + " TEXT,"
                + KEY_STATE + " TEXT,"
                + KEY_TEACHER + " TEXT,"
                + KEY_TAKES_ALL_DAY + " BOOLEAN,"
                + KEY_TYPE + " TEXT"
                + ")";
        db.execSQL(CREATE_CALENDAR_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: New Version!");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALENDAR);
        onCreate(db);
    }

    public void addItems(Appointment[] appointments, Context context) {
        if (appointments.length == 0 || appointments == null) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();

        Log.d(TAG, "addItems: amount of items: " + appointments.length);
        String day = "";

        for (Appointment item :
                appointments) {
            Integer id = item.id;

            /*
            Fixing the Timezone Bug

            String startDateString;
            try {
                startDateString = DateUtil.dateToString(DateUtils.addHours(item.startDate, 2));
            } catch (ParseException e) {
                startDateString = null;
                e.printStackTrace();
            }
            String endDateString;
            String secondEndDateString;
            try {
                //Not adding 2 hours to prevent appointments from being showed one day too long.
                endDateString = DateUtil.dateToString(DateUtils.addHours(item.endDate, 1));
                secondEndDateString = DateUtil.dateToString(DateUtils.addHours(item.endDate, 2));
            } catch (ParseException e) {
                endDateString = null;
                secondEndDateString = null;
                e.printStackTrace();
            }
            /*
             End of the bug fix
             */
            String startDateString;
            String endDateString;
            String secondEndDateString;

            item.startDate = DateUtils.fixTimeDifference(item.startDate, false, context);
            item.endDate = DateUtils.fixTimeDifference(item.endDate, false, context);
            try {
                item.startDateString = DateUtil.dateToString(item.startDate);
                item.endDateString = DateUtil.dateToString(item.endDate);
            } catch (ParseException e) {
                e.printStackTrace();
                return;
            }


            try {
                startDateString = DateUtil.dateToString(item.startDate);
                endDateString = DateUtil.dateToString(item.endDate);
                secondEndDateString = DateUtil.dateToString(item.endDate);
            } catch (ParseException e) {
                e.printStackTrace();
                return;
            }

            if (!day.equals(startDateString.replaceAll("-", "").substring(0, 8))) {
                deleteAppointmentByDateString(startDateString);
            }
            day = startDateString.replaceAll("-", "").substring(0, 8);

            contentValues.put(KEY_CALENDAR_ID, id);
            contentValues.put(KEY_DESC, item.description);
            contentValues.put(KEY_CLASS_ROOMS, new Gson().toJson(item.classrooms));
            contentValues.put(KEY_CONTENT, item.content);
            contentValues.put(KEY_END, item.endDateString);
            contentValues.put(KEY_FINISHED, item.finished);
            contentValues.put(KEY_FORMATTED_END, endDateString.replaceAll("-", "").substring(0, 8));
            contentValues.put(KEY_FORMATTED_START, startDateString.replaceAll("-", "").substring(0, 8));
            contentValues.put(KEY_FORMATTED_END_2, secondEndDateString.replaceAll("[T:-]", "").substring(4, 12));
            contentValues.put(KEY_FORMATTED_START_2, startDateString.replaceAll("[T:-]", "").substring(4, 12));
            try {
                contentValues.put(KEY_INFO_TYPE, item.infoType.getID());
            } catch (NullPointerException e) {
                Log.e(TAG, "addItems: No infotype!", e);
                contentValues.put(KEY_INFO_TYPE, 0);
            }
            contentValues.put(KEY_LINKS, new Gson().toJson(item.links));
            contentValues.put(KEY_LOCATION, item.location);
            contentValues.put(KEY_PERIOD_FROM, item.periodFrom);
            contentValues.put(KEY_PERIOD_TO, item.periodUpToAndIncluding);
            contentValues.put(KEY_START, item.startDateString);
            contentValues.put(KEY_STATE, item.classState);
            contentValues.put(KEY_SUBJECTS, new Gson().toJson(item.subjects));
            contentValues.put(KEY_TEACHER, new Gson().toJson(item.teachers));
            contentValues.put(KEY_TAKES_ALL_DAY, item.takesAllDay);
            contentValues.put(KEY_TYPE, item.type.getID());


            db.insert(TABLE_CALENDAR, null, contentValues);

        }

    }

    public void deleteAppointmentByDate(Date date) {
        Integer dateInt = parseInt(formatDate(date, "yyyyMMdd"));
        deleteAppointmentByDateInt(dateInt);
    }

    public void deleteAppointmentByDateString(String date) {
        Integer dateInt = parseInt(date.replaceAll("[T:Z.-]", "").substring(0, 8));
        deleteAppointmentByDateInt(dateInt);
    }

    private void deleteAppointmentByDateInt(Integer date) {
        SQLiteDatabase db = this.getWritableDatabase();
        String Query = "DELETE FROM " + TABLE_CALENDAR + " WHERE " + KEY_FORMATTED_START + " <= " + date + " AND "
                + KEY_FORMATTED_END + " >= " + date;
        Log.d(TAG, "deleteAppointmentByDateInt: Query: " + Query);
        db.execSQL(Query);

    }

    public void finishAppointment(Appointment appointment) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_FINISHED, appointment.finished);

        db.update(TABLE_CALENDAR, contentValues, KEY_CALENDAR_ID + "=" + appointment.id, null);

    }
    
    public Appointment[] getAppointmentsByDate(Date date){
        SQLiteDatabase db = this.getWritableDatabase();
        Integer dateInt = parseInt(formatDate(date, "yyyyMMdd"));
        String Query = "SELECT * FROM " + TABLE_CALENDAR + " WHERE " + KEY_FORMATTED_START + " <= " + dateInt + " AND "
                + KEY_FORMATTED_END + " >= " + dateInt;
        Cursor cursor = db.rawQuery(Query, null);
        Log.d(TAG, "getAppointmentsByDate: Query: " + Query);
        Log.d(TAG, "getAppointmentsByDate: amount of items: " + cursor.getCount());
        Appointment[] results = new Appointment[cursor.getCount()];
        int i = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Gson gson = new Gson();
                    Appointment appointment = new Appointment();
                    appointment.id = cursor.getInt(cursor.getColumnIndex(KEY_CALENDAR_ID));
                    appointment.startDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_START)), "yyyy-MM-dd'T'HH:mm:ss.0000000'Z'");
                    appointment.classrooms = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_CLASS_ROOMS)), Classroom[].class);
                    appointment.content = cursor.getString(cursor.getColumnIndex(KEY_CONTENT));
                    appointment.endDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_END)), "yyyy-MM-dd'T'HH:mm:ss.0000000'Z'");
                    appointment.finished = cursor.getInt(cursor.getColumnIndex(KEY_FINISHED)) > 0;
                    appointment.infoType = InfoType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_INFO_TYPE)));
                    appointment.links = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_LINKS)), Link[].class);
                    appointment.location = cursor.getString(cursor.getColumnIndex(KEY_LOCATION));
                    appointment.periodFrom = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_FROM));
                    appointment.periodUpToAndIncluding = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_TO));
                    appointment.description = cursor.getString(cursor.getColumnIndex(KEY_DESC));
                    appointment.classState = cursor.getInt(cursor.getColumnIndex(KEY_STATE));
                    appointment.subjects = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_SUBJECTS)), SubSubject[].class);
                    appointment.teachers = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_TEACHER)), Teacher[].class);
                    appointment.type = AppointmentType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_TYPE)));

                    results[i] = appointment;
                    i++;
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        return results;
    }

    public Appointment[] getHomework(Date now) {
        SQLiteDatabase db = this.getWritableDatabase();
        Integer dateStart = Integer.parseInt(formatDate(now, "yyyyMMdd"));
        Integer dateEnd = Integer.parseInt(formatDate(addDays(now, 14), "yyyyMMdd"));
        String Query = "SELECT * FROM " + TABLE_CALENDAR + " WHERE " + KEY_FORMATTED_START + " <= " + dateEnd + " AND "
                + KEY_FORMATTED_END + " >= " + dateStart + " AND " + KEY_CONTENT + " IS NOT NULL";

        Cursor cursor = db.rawQuery(Query, null);
        Log.d(TAG, "getAppointmentsByDate: Query: " + Query);
        Log.d(TAG, "getAppointmentsByDate: amount of items: " + cursor.getCount());
        Appointment[] results = new Appointment[cursor.getCount()];
        int i = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Gson gson = new Gson();
                    Appointment appointment = new Appointment();
                    appointment.id = cursor.getInt(cursor.getColumnIndex(KEY_CALENDAR_ID));
                    appointment.startDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_START)), "yyyy-MM-dd'T'HH:mm:ss.0000000'Z'");
                    appointment.classrooms = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_CLASS_ROOMS)), Classroom[].class);
                    appointment.content = cursor.getString(cursor.getColumnIndex(KEY_CONTENT));
                    appointment.endDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_END)), "yyyy-MM-dd'T'HH:mm:ss.0000000'Z'");
                    appointment.finished = cursor.getInt(cursor.getColumnIndex(KEY_FINISHED)) > 0;
                    appointment.infoType = InfoType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_INFO_TYPE)));
                    appointment.links = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_LINKS)), Link[].class);
                    appointment.location = cursor.getString(cursor.getColumnIndex(KEY_LOCATION));
                    appointment.periodFrom = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_FROM));
                    appointment.periodUpToAndIncluding = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_TO));
                    appointment.description = cursor.getString(cursor.getColumnIndex(KEY_DESC));
                    appointment.classState = cursor.getInt(cursor.getColumnIndex(KEY_STATE));
                    appointment.subjects = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_SUBJECTS)), SubSubject[].class);
                    appointment.teachers = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_TEACHER)), Teacher[].class);
                    appointment.type = AppointmentType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_TYPE)));

                    results[i] = appointment;
                    i++;
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        return results;
    }

    public Appointment[] getNotificationAppointments() {
        SQLiteDatabase db = this.getWritableDatabase();
        Date now = getToday();
        Date start = addMinutes(now, 25);
        Date end = addMinutes(now, -15);

        Integer startdateInt = parseInt(formatDate(start, "MMddHHmm"));
        Integer enddateInt = parseInt(formatDate(end, "MMddHHmm"));
        String Query = "SELECT * FROM " + TABLE_CALENDAR + " WHERE " + KEY_FORMATTED_START_2 + " <= " + startdateInt + " AND "
                + KEY_FORMATTED_END_2 + " >= " + startdateInt + " AND " + KEY_FORMATTED_START_2 + " >= " + enddateInt;
        Log.d(TAG, "getNotificationAppointments: Query: " + Query);
        Cursor cursor = db.rawQuery(Query, null);

        Appointment[] results = new Appointment[cursor.getCount()];
        int i = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Gson gson = new Gson();
                    Appointment appointment = new Appointment();
                    appointment.id = cursor.getInt(cursor.getColumnIndex(KEY_CALENDAR_ID));
                    appointment.startDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_START)), "yyyy-MM-dd'T'HH:mm:ss.0000000'Z'");
                    appointment.classrooms = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_CLASS_ROOMS)), Classroom[].class);
                    appointment.content = cursor.getString(cursor.getColumnIndex(KEY_CONTENT));
                    appointment.endDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_END)), "yyyy-MM-dd'T'HH:mm:ss.0000000'Z'");
                    appointment.finished = cursor.getInt(cursor.getColumnIndex(KEY_FINISHED)) > 0;
                    appointment.infoType = InfoType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_INFO_TYPE)));
                    appointment.links = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_LINKS)), Link[].class);
                    appointment.location = cursor.getString(cursor.getColumnIndex(KEY_LOCATION));
                    appointment.periodFrom = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_FROM));
                    appointment.periodUpToAndIncluding = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_TO));
                    appointment.description = cursor.getString(cursor.getColumnIndex(KEY_DESC));
                    appointment.classState = cursor.getInt(cursor.getColumnIndex(KEY_STATE));
                    appointment.subjects = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_SUBJECTS)), SubSubject[].class);
                    appointment.teachers = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_TEACHER)), Teacher[].class);
                    appointment.type = AppointmentType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_TYPE)));

                    results[i] = appointment;
                    i++;
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        return results;
    }

    public Appointment[] getSilentAppointments(int margin) {
        SQLiteDatabase db = this.getWritableDatabase();
        Integer startdateInt = parseInt(formatDate(addMinutes(getToday(), margin), "MMddHHmm"));
        Integer enddateInt = parseInt(formatDate(addMinutes(getToday(), -margin), "MMddHHmm"));
        String Query = "SELECT * FROM " + TABLE_CALENDAR + " WHERE " + KEY_FORMATTED_START_2 + " <= " + startdateInt + " AND "
                + KEY_FORMATTED_END_2 + " >= " + enddateInt;
        Cursor cursor = db.rawQuery(Query, null);

        Appointment[] results = new Appointment[cursor.getCount()];
        int i = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Appointment appointment = new Appointment();
                    appointment.id = cursor.getInt(cursor.getColumnIndex(KEY_CALENDAR_ID));
                    appointment.type = AppointmentType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_TYPE)));

                    results[i] = appointment;
                    i++;
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        return results;
    }

    public void deleteAppointment(Appointment appointment) {
        deleteAppointment(appointment.id);
    }

    public void deleteAppointment(int id) {
        Log.d(TAG, "deleteAppointment() called with: " + "id = [" + id + "]");

        SQLiteDatabase db = this.getWritableDatabase();
        String Query = "DELETE FROM " + TABLE_CALENDAR + " WHERE " + KEY_CALENDAR_ID + " = " + id + "";
        Log.d(TAG, "deleteAppointment: " + Query);
        db.execSQL(Query);
    }

    public boolean CheckInDB(String TableName, String dbfield, String fieldValue) {

        SQLiteDatabase db = this.getWritableDatabase();
        String Query = "Select * from " + TableName + " where " + dbfield + " = " + fieldValue;
        Cursor cursor = db.rawQuery(Query, null);
        if (cursor.getCount() <= 0) {
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
    }

    public void removeAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CALENDAR, null, null);
    }

    public void removeAll(SQLiteDatabase db) {
        db.delete(TABLE_CALENDAR, null, null);
    }
}
