package at.yousong.yousong_api.song;

import at.yousong.yousong_api.artist.Artist;

public interface SongProjection {
    Long getId();
    String getTitle();
    String getGenre();
    int getLength();
    Artist getArtist();
}
