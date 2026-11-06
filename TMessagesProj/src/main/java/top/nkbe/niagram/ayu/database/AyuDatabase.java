/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package top.nkbe.niagram.ayu.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import top.nkbe.niagram.ayu.database.dao.DeletedMessageDao;
import top.nkbe.niagram.ayu.database.dao.EditedMessageDao;
import top.nkbe.niagram.ayu.database.dao.LastSeenDao;
import top.nkbe.niagram.ayu.database.entities.DeletedMessage;
import top.nkbe.niagram.ayu.database.entities.DeletedMessageReaction;
import top.nkbe.niagram.ayu.database.entities.EditedMessage;
import top.nkbe.niagram.ayu.database.entities.LastSeenEntity;

@Database(entities = {
        EditedMessage.class,
        DeletedMessage.class,
        DeletedMessageReaction.class,
        LastSeenEntity.class
}, version = AyuDatabase.VERSION)
public abstract class AyuDatabase extends RoomDatabase {
    public static final int MIN_SUPPORTED_VERSION = 21;
    public static final int VERSION = 26;

    public abstract EditedMessageDao editedMessageDao();

    public abstract DeletedMessageDao deletedMessageDao();

    public abstract LastSeenDao lastSeenDao();
}
