package at.yousong.yousong_api.song;

import at.yousong.yousong_api.artist.Artist;
import at.yousong.yousong_api.artist.ArtistRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(20)
public class DataLoader implements CommandLineRunner {

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;

    public DataLoader(SongRepository songRepository, ArtistRepository artistRepository) {
        this.songRepository = songRepository;
        this.artistRepository = artistRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (songRepository.count() > 0) {
            return; // schon Daten vorhanden
        }

        Artist ed = getOrCreateArtist("Ed Sheeran", "UK singer-songwriter");
        Artist queen = getOrCreateArtist("Queen", "Legendary British rock band");
        Artist taylor = getOrCreateArtist("Taylor Swift", "US singer-songwriter and producer");

        songRepository.save(new Song(null, "Shape of You", "Pop", 233, ed, null));
        songRepository.save(new Song(null, "Perfect", "Pop", 263, ed, null));
        songRepository.save(new Song(null, "Bohemian Rhapsody", "Rock", 354, queen, null));
        songRepository.save(new Song(null, "Love Story", "Country Pop", 235, taylor, null));
    }

    private Artist getOrCreateArtist(String name, String description) {
        return artistRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> artistRepository.save(new Artist(null, name, description)));
    }
}
