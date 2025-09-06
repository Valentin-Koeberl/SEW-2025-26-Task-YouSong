package at.yousong.yousong_api.song;

import at.yousong.yousong_api.artist.Artist;
import at.yousong.yousong_api.user.Benutzer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title must not be blank.")
    @Size(max = 200, message = "Title must be at most 200 characters.")
    private String title;

    @NotBlank(message = "Genre must not be blank.")
    @Size(max = 80, message = "Genre must be at most 80 characters.")
    private String genre;

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

    // Wichtig: nicht serialisieren
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private Benutzer owner;

    public Song() {}

    public Song(Long id, String title, String genre, int length, Artist artist, String musicData) {
        this.id = id; this.title = title; this.genre = genre; this.length = length; this.artist = artist; this.musicData = musicData;
    }

    public Song(Long id, String title, String genre, int length, Artist artist, String musicData, Benutzer owner) {
        this(id, title, genre, length, artist, musicData);
        this.owner = owner;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

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
