package at.yousong.yousong_api.song;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {

    // Suche im Titel ODER im Artist-Namen (case-insensitive)
    List<Song> findByTitleContainingIgnoreCaseOrArtist_NameContainingIgnoreCase(String title, String artistName);
}
