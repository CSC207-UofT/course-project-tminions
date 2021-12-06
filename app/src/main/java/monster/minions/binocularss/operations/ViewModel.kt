package monster.minions.binocularss.operations

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.room.RoomDatabase
import com.prof.rssparser.Channel
import com.prof.rssparser.Parser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import monster.minions.binocularss.activities.MainActivity
import monster.minions.binocularss.dataclasses.Article
import monster.minions.binocularss.dataclasses.Feed
import monster.minions.binocularss.dataclasses.FeedGroup
import monster.minions.binocularss.room.AppDatabase
import monster.minions.binocularss.room.DatabaseGateway
import monster.minions.binocularss.room.FeedDao

/**
 * Asynchronous execution class that runs XML parser code off of the main thread to not interrupt UI
 */
class PullFeed(context: Context, feedGroup: FeedGroup) : ViewModel() {
    var localContext = context
    var isRefreshing = MutableStateFlow(false)

    // FeedGroup object
    private var localFeedGroup: FeedGroup = feedGroup

    // Room database variables
    private lateinit var databaseGateway: DatabaseGateway

    /**
     * Call the required functions to update the Rss feed.
     *
     * @param parser A parser with preconfigured settings.
     */
    fun updateRss(parser: Parser) {
        val scope = CoroutineScope(Job() + Dispatchers.IO)
        scope.launch {
            isRefreshing.value = true
            // Update feedGroup variable.
            val fetchFeed = FetchFeed()
            localFeedGroup = fetchFeed.pullRss(localFeedGroup, parser)

            // Update DB with updated feeds.
            databaseGateway = DatabaseGateway(context=localContext)
            databaseGateway.addFeeds(localFeedGroup.feeds)

            // Update list states in MainActivity.
            MainActivity.articleList.value = sortArticlesByDate(getAllArticles(localFeedGroup))
            MainActivity.bookmarkedArticleList.value = sortArticlesByDate(getBookmarkedArticles(localFeedGroup))
            MainActivity.currentFeedArticles.value = sortArticlesByDate(getArticlesFromFeed(MainActivity.currentFeed))
            MainActivity.readArticleList.value = sortArticlesByReadDate(getReadArticles(localFeedGroup))
            MainActivity.feedList.value = sortFeedsByTitle(localFeedGroup.feeds)

            isRefreshing.value = false

            MainActivity.updateFeedGroup(databaseGateway.read())
        }
    }

}