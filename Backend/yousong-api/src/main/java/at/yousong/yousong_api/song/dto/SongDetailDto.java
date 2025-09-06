package at.yousong.yousong_api.song.dto;

public class SongDetailDto {
    public Long id;
    public String title;
    public String genre;
    public int length;
    public Long version;
    public ArtistRefDto artist;
    public String musicData;

    public SongDetailDto(Long id, String title, String genre, int length, Long version, ArtistRefDto artist, String musicData) {
        this.id = id; this.title = title; this.genre = genre; this.length = length; this.version = version; this.artist = artist; this.musicData = musicData;
    }
}
