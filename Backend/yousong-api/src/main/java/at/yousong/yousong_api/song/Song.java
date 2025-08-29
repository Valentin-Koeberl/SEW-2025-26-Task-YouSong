package at.yousong.yousong_api.song;

import jakarta.persistence.*;

@Entity
@Table(name = "songs")
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String artist;
    private String genre;
    private int length;

    public Song() {}

    public Song(Long id, String title, String artist, String genre, int length) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.genre = genre;
        this.length = length;
    }

    // Getter und Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }
}
