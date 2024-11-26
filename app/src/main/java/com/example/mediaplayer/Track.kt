package com.example.mediaplayer

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

// Room Entity для хранения информации о треках в локальной базе данных
@Entity(tableName = "track")
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Уникальный идентификатор
    val title: String, // Название трека
    val artist: String, // Исполнитель
    val path: String, // Путь к файлу трека (локальный или URL)
    val coverPath: String // Путь к обложке (локальный или URL)
) : Parcelable {

    // Конструктор для создания объекта из Parcel
    private constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        title = parcel.readString().orEmpty(),
        artist = parcel.readString().orEmpty(),
        path = parcel.readString().orEmpty(),
        coverPath = parcel.readString().orEmpty()
    )

    // Запись объекта в Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeString(path)
        parcel.writeString(coverPath)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Track> {
        override fun createFromParcel(parcel: Parcel): Track = Track(parcel)
        override fun newArray(size: Int): Array<Track?> = arrayOfNulls(size)
    }
}
