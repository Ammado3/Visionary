package com.o3.vanguard.visionary.core.database

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object FtsCallback : RoomDatabase.Callback() {
    override suspend fun onCreate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE VIRTUAL TABLE IF NOT EXISTS media_fts USING fts5(
                 search_text, labels_text,
                 content='media_text', content_rowid='mediaId',
                 tokenize='unicode61 remove_diacritics 2', prefix='2 3')"""
        )
        connection.execSQL(
            """CREATE TRIGGER IF NOT EXISTS media_text_ai AFTER INSERT ON media_text BEGIN
                 INSERT INTO media_fts(rowid, search_text, labels_text)
                 VALUES (new.mediaId, new.searchText, new.labelsText); END"""
        )
        connection.execSQL(
            """CREATE TRIGGER IF NOT EXISTS media_text_ad AFTER DELETE ON media_text BEGIN
                 INSERT INTO media_fts(media_fts, rowid, search_text, labels_text)
                 VALUES ('delete', old.mediaId, old.searchText, old.labelsText); END"""
        )
        connection.execSQL(
            """CREATE TRIGGER IF NOT EXISTS media_text_au AFTER UPDATE ON media_text BEGIN
                 INSERT INTO media_fts(media_fts, rowid, search_text, labels_text)
                 VALUES ('delete', old.mediaId, old.searchText, old.labelsText);
                 INSERT INTO media_fts(rowid, search_text, labels_text)
                 VALUES (new.mediaId, new.searchText, new.labelsText); END"""
        )
    }
}
