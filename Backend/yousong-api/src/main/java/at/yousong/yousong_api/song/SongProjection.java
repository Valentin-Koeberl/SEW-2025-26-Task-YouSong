package at.yousong.yousong_api.song;

import at.yousong.yousong_api.artist.Artist;
import java.util.List;

public interface SongProjection {
    Long getId();
    String getTitle();
    int getLength();
    Artist getArtist();
    List<String> getGenres();
}
