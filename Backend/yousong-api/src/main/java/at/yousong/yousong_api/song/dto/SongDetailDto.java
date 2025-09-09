package at.yousong.yousong_api.song.dto;

import java.util.List;

public class SongDetailDto {
    public Long id;
    public String title;
    public List<String> genres;
    public int length;
    public Long version;
    public ArtistRefDto artist;
    public String musicData;

    public SongDetailDto(Long id, String title, List<String> genres, int length, Long version, ArtistRefDto artist, String musicData) {
        this.id = id; this.title = title; this.genres = genres; this.length = length; this.version = version; this.artist = artist; this.musicData = musicData;
    }
}
