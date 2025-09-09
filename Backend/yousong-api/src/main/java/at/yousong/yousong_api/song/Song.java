package at.yousong.yousong_api.song;

import at.yousong.yousong_api.artist.Artist;
import at.yousong.yousong_api.user.Benutzer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title must not be blank.")
    @Column(nullable = false, length = 200)
    private String title;

    @ElementCollection
    @CollectionTable(
            name = "song_genres",
            joinColumns = @JoinColumn(name = "song_id", foreignKey = @ForeignKey(name = "fk_song_genres_song"))
    )
    @Column(name = "genre", nullable = false, length = 80)
    private List<String> genres = new ArrayList<>();

    @Min(value = 1, message = "Length must be at least 1 second.")
    private int length;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false, foreignKey = @ForeignKey(name = "fk_song_artist"))
    @NotNull(message = "Artist must be provided.")
    private Artist artist;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String musicData;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private Benutzer owner;

    public Song() {}

    public Song(Long id, String title, List<String> genres, int length, Artist artist, String musicData) {
        this.id = id; this.title = title; this.genres = genres; this.length = length; this.artist = artist; this.musicData = musicData;
    }

    public Song(Long id, String title, List<String> genres, int length, Artist artist, String musicData, Benutzer owner) {
        this(id, title, genres, length, artist, musicData);
        this.owner = owner;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }

    public Artist getArtist() { return artist; }
    public void setArtist(Artist artist) { this.artist = artist; }

    public String getMusicData() { return musicData; }
    public void setMusicData(String musicData) { this.musicData = musicData; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Benutzer getOwner() { return owner; }
    public void setOwner(Benutzer owner) { this.owner = owner; }
}
