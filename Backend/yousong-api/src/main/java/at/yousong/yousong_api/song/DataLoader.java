package at.yousong.yousong_api.song;

import at.yousong.yousong_api.artist.Artist;
import at.yousong.yousong_api.artist.ArtistRepository;
import at.yousong.yousong_api.user.Benutzer;
import at.yousong.yousong_api.user.BenutzerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(20)
public class DataLoader implements CommandLineRunner {

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final BenutzerRepository benutzerRepository;

    public DataLoader(SongRepository songRepository, ArtistRepository artistRepository, BenutzerRepository benutzerRepository) {
        this.songRepository = songRepository;
        this.artistRepository = artistRepository;
        this.benutzerRepository = benutzerRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (songRepository.count() > 0) {
            return;
        }

        Artist ed = getOrCreateArtist("Ed Sheeran", "UK singer-songwriter");
        Artist queen = getOrCreateArtist("Queen", "Legendary British rock band");
        Artist taylor = getOrCreateArtist("Taylor Swift", "US singer-songwriter and producer");

        Benutzer owner = benutzerRepository.findByUsername("hugo").orElse(null);

        songRepository.save(new Song(null, "Shape of You", List.of("Pop"), 233, ed, null, owner));
        songRepository.save(new Song(null, "Perfect", List.of("Pop"), 263, ed, null, owner));
        songRepository.save(new Song(null, "Bohemian Rhapsody", List.of("Rock"), 354, queen, null, owner));
        songRepository.save(new Song(null, "Love Story", List.of("Country Pop","Pop"), 235, taylor, null, owner));
    }

    private Artist getOrCreateArtist(String name, String description) {
        return artistRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> artistRepository.save(new Artist(null, name, description)));
    }
}
