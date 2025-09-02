package at.yousong.yousong_api.song;

import at.yousong.yousong_api.artist.Artist;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 120)
    private String genre;

    private int length; // seconds

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    public Song() {}

    public Song(Long id, String title, String genre, int length, Artist artist) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.length = length;
        this.artist = artist;
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
}
