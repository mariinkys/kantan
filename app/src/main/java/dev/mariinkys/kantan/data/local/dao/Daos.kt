package dev.mariinkys.kantan.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.mariinkys.kantan.data.local.entity.KanjiEntity
import dev.mariinkys.kantan.data.local.entity.TermEntity
import dev.mariinkys.kantan.data.local.entity.TermFtsEntity

@Dao
interface TermDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(terms: List<TermEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFts(entries: List<TermFtsEntity>)

    /**
     * Prefix search for Japanese / romaji-converted kana input.
     *
     * Ordering: exact reading match first, then by reading length ascending
     * (shorter = more specific match), then score descending.
     * This ensures e.g. "簡単" (かんたん) ranks above "簡単に" (かんたんに)
     * when searching for "かんたん".
     */
    @Query(
        """
        SELECT * FROM terms
        WHERE expression LIKE :prefix || '%'
           OR reading    LIKE :prefix || '%'
        ORDER BY
            CASE WHEN reading = :prefix OR expression = :prefix THEN 0 ELSE 1 END ASC,
            LENGTH(reading) ASC,
            score DESC
        LIMIT :limit
    """
    )
    suspend fun searchByPrefix(prefix: String, limit: Int = 60): List<TermEntity>

    @Query(
        """
        SELECT t.* FROM terms t
        WHERE t.expression IN (
            SELECT expression FROM terms_fts WHERE terms_fts MATCH :query
        )
        ORDER BY 
            /* TIER 1: Exact matches for the search term in definitions */
            (t.definitions_text = :rawQuery) DESC,
            
            /* TIER 2: Definition starts with the search term (primary meaning) */
            (t.definitions_text LIKE :rawQuery || '%') DESC,
            
            /* TIER 3: Commonality score from the dictionary */
            t.score DESC,
            
            /* TIER 4: Favor shorter Japanese words to avoid compound word noise */
            LENGTH(t.expression) ASC
        LIMIT :limit
    """
    )
    suspend fun searchByFts(query: String, rawQuery: String, limit: Int): List<TermEntity>

    @Query("SELECT COUNT(*) FROM terms")
    suspend fun count(): Int
}

@Dao
interface KanjiDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(kanji: List<KanjiEntity>)

    @Query("SELECT * FROM kanji WHERE character = :char LIMIT 1")
    suspend fun getByCharacter(char: String): KanjiEntity?

    @Query("SELECT * FROM kanji WHERE character IN (:chars)")
    suspend fun getByCharacters(chars: List<String>): List<KanjiEntity>

    @Query("SELECT COUNT(*) FROM kanji")
    suspend fun count(): Int
}