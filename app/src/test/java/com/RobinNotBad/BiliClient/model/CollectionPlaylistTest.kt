package com.RobinNotBad.BiliClient.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 合集展平成播放列表的纯函数测试（26.10.15）。
 *
 * 这条顺序是"以合集播放"相对收藏夹连播的唯一价值所在——收藏夹的顺序是
 * "你什么时候收藏的"，与合集官方顺序无关。展平错了，用户看到的顺序就是错的，
 * 而这种错在真机上很难一眼看出来（合集动辄上百集）。
 */
class CollectionPlaylistTest {

    private fun episode(aid: Long, cid: Long, title: String): Collection.Episode =
        Collection.Episode().apply {
            this.aid = aid
            this.cid = cid
            this.title = title
        }

    private fun section(title: String, vararg eps: Collection.Episode): Collection.Section =
        Collection.Section().apply {
            this.title = title
            episodes = eps.toList()
        }

    @Test
    fun 有分节_跨节展平且保持节与集的自然顺序() {
        val c = Collection().apply {
            sections = listOf(
                section("第一节", episode(11, 111, "A1"), episode(12, 112, "A2")),
                section("第二节", episode(21, 211, "B1"))
            )
        }
        val items = Collection.toPlaylist(c)
        assertEquals(listOf(11L, 12L, 21L), items.map { it.aid })
        assertEquals(listOf(111L, 112L, 211L), items.map { it.cid })
        assertEquals(listOf("A1", "A2", "B1"), items.map { it.title })
    }

    @Test
    fun 没有分节_退回cards() {
        //"收藏的合集"（getFavoritedCollections）只填 cards、sections 为空
        val c = Collection().apply {
            cards = listOf(
                VideoCard("C1", "up", "1万观看", "cover", 31L, "BV1", 311L),
                VideoCard("C2", "up", "1万观看", "cover", 32L, "BV2", 312L)
            )
        }
        val items = Collection.toPlaylist(c)
        assertEquals(listOf(31L, 32L), items.map { it.aid })
        assertEquals(listOf(311L, 312L), items.map { it.cid })
    }

    @Test
    fun sections存在但展不出条目时_仍退回cards() {
        val c = Collection().apply {
            sections = listOf(section("空节"))
            cards = listOf(VideoCard("C1", "up", "1万观看", "cover", 41L, "BV1", 411L))
        }
        assertEquals(listOf(41L), Collection.toPlaylist(c).map { it.aid })
    }

    @Test
    fun aid或cid缺失的脏数据_跳过而不是留下会取流失败的条目() {
        val c = Collection().apply {
            sections = listOf(
                section(
                    "节",
                    episode(0, 111, "无 aid"),
                    episode(12, 0, "无 cid"),
                    episode(13, 113, "正常")
                )
            )
        }
        assertEquals(listOf(13L), Collection.toPlaylist(c).map { it.aid })
    }

    @Test
    fun 空合集与null_都返回空列表不崩溃() {
        assertEquals(0, Collection.toPlaylist(Collection()).size)
        assertEquals(0, Collection.toPlaylist(null).size)
    }

    @Test
    fun 节为null_跳过而不是崩溃() {
        val c = Collection().apply { sections = listOf(section("节", episode(11, 111, "A1"))) }
        @Suppress("UNCHECKED_CAST")
        c.sections[0].episodes = listOf(episode(11, 111, "A1"), null) as List<Collection.Episode>
        assertEquals(listOf(11L), Collection.toPlaylist(c).map { it.aid })
    }
}
