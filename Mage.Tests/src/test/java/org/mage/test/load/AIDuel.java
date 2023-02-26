package org.mage.test.load;

import mage.cards.Card;
import mage.cards.decks.Deck;
import mage.cards.decks.DeckCardLists;
import mage.cards.decks.importer.DeckImporter;
import mage.cards.repository.CardScanner;
import mage.constants.*;
import mage.game.match.MatchOptions;
import mage.players.PlayerType;
import mage.remote.Connection;
import mage.remote.MageRemoteException;
import mage.remote.Session;
import mage.remote.SessionImpl;
import mage.util.RandomUtil;
import mage.view.*;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mage.test.utils.DeckTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Intended to test Mage server under different load patterns.
 * <p>
 * These tests do use server started separately, so Mage server should be
 * started before running them. In case you want to debug these tests, use
 * -Ddebug.mage that would disable client-server request timeout.
 * <p>
 * Then it's also better to use -Xms256M -Xmx512M JVM options for these tests.
 *
 * @author JayDi85
 */
public class AIDuel {

    private static final Logger logger = Logger.getLogger(LoadTest.class);

    private static final String TEST_SERVER = "localhost";
    private static final int TEST_PORT = 17171;
    private static final String TEST_PROXY_TYPE = "None";
    private static final String TEST_USER_NAME = "user";
    private static final String TEST_AI_GAME_MODE = "Two Player Duel";

    private static final String TEST_AI_DECK_TYPE = "Limited";
    //private static final String TEST_AI_RANDOM_DECK_SETS = "NEO"; // set for random generated decks (empty for all sets usage)
    private static final String TEST_AI_CUSTOM_DECK_PATH_1 = ""; // custom deck file instead random for player 1 (empty for random)
    private static final String TEST_AI_CUSTOM_DECK_PATH_2 = ""; // custom deck file instead random for player 2 (empty for random)

    public static void main(String[] args) {
        logger.info("hey I entered the funciton at least!!!!!! new vewrsion");
        String gameName ="AI Duel";
        String deckColors = "UB";
        String deckAllowedSets ="M20";
        //LoadTestGameResult gameResult = new LoadTestGameResult(0, "ai_test", 1);


        
        Assert.assertFalse("need deck colors", deckColors.isEmpty());
        Assert.assertFalse("need allowed sets", deckAllowedSets.isEmpty());

        // monitor and game source
        //LoadPlayer monitor = new LoadPlayer("m", true);
        String userName = TEST_USER_NAME + RandomUtil.nextInt(10000);
        Connection connection = createSimpleConnection(userName);
        SimpleMageClient client = new SimpleMageClient(true);
        Session session = new SessionImpl(client);

        session.connect(connection);
        client.setSession(session);
        UUID roomID = session.getMainRoomId();



        // game by monitor
        GameTypeView gameType = prepareGameType(session);
        MatchOptions gameOptions = createSimpleGameOptionsForAI(gameType, session, gameName);
        TableView game = session.createTable(roomID, gameOptions);
        UUID tableId = game.getTableId();

        // deck load
        DeckCardLists deckListRandom = DeckTestUtils.buildRandomDeckAndInitCards(deckColors, false, deckAllowedSets);
        DeckCardLists deckList1 = deckListRandom;
        DeckCardLists deckList2 = deckListRandom;
        if (!TEST_AI_CUSTOM_DECK_PATH_1.isEmpty()) {
            deckList1 = DeckImporter.importDeckFromFile(TEST_AI_CUSTOM_DECK_PATH_1, false);
            Assert.assertFalse("Can't load custom deck 1 from " + TEST_AI_CUSTOM_DECK_PATH_1, deckList1.getCards().isEmpty());
        }
        if (!TEST_AI_CUSTOM_DECK_PATH_2.isEmpty()) {
            deckList2 = DeckImporter.importDeckFromFile(TEST_AI_CUSTOM_DECK_PATH_2, false);
            Assert.assertFalse("Can't load custom deck 2 from " + TEST_AI_CUSTOM_DECK_PATH_2, deckList2.getCards().isEmpty());
        }

        Optional<TableView> checkGame;

        // join AI
        Assert.assertTrue(session.joinTable(roomID, tableId, "ai_1", PlayerType.COMPUTER_MAD, 5, deckList1, ""));
        Assert.assertTrue(session.joinTable(roomID, tableId, "ai_2", PlayerType.COMPUTER_MAD, 5, deckList2, ""));

        // match start
        Assert.assertTrue(session.startMatch(roomID, tableId));

        // playing until game over
        //gameResult.start();
        boolean startToWatching = false;
        
        while (true) {
            GameView gameView = client.getLastGameView();

            checkGame = session.getTable(roomID,tableId);
            TableState state = checkGame.get().getTableState();

            logger.warn((gameView != null ? ", turn " + gameView.getTurn() + ", " + gameView.getStep().toString() : "")
                    + (gameView != null ? ", active " + gameView.getActivePlayerName() : ""));

            if (state == TableState.FINISHED) {
                //gameResult.finish(gameView);
                break;
            }

            if (!startToWatching && state == TableState.DUELING) {
                Assert.assertTrue(session.watchGame(checkGame.get().getGames().iterator().next()));
                startToWatching = true;
            }

            if (gameView != null) {
                for (PlayerView p : gameView.getPlayers()) {
                    logger.info(p.getName() + " - Life=" + p.getLife() + "; Lib=" + p.getLibraryCount());
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private static Connection createSimpleConnection(String username) {
        Connection con = new Connection();
        con.setUsername(username);
        con.setHost(TEST_SERVER);
        con.setPort(TEST_PORT);
        Connection.ProxyType proxyType = Connection.ProxyType.valueByText(TEST_PROXY_TYPE);
        con.setProxyType(proxyType);
        return con;
    }

    private static MatchOptions createSimpleGameOptions(String gameName, GameTypeView gameTypeView, Session session, PlayerType playersType) {
        MatchOptions options = new MatchOptions(gameName, gameTypeView.getName(), true, 2);

        options.getPlayerTypes().add(playersType);
        options.getPlayerTypes().add(playersType);

        Assert.assertTrue("Can't find game type on the server: " + TEST_AI_DECK_TYPE,
                Arrays.asList(session.getDeckTypes()).contains(TEST_AI_DECK_TYPE));
        
        options.setDeckType(TEST_AI_DECK_TYPE);
        options.setLimited(false);
        options.setAttackOption(MultiplayerAttackOption.MULTIPLE);
        options.setRange(RangeOfInfluence.ALL);
        options.setWinsNeeded(1);
        options.setMatchTimeLimit(MatchTimeLimit.MIN__15);
        return options;
    }

    private static MatchOptions createSimpleGameOptionsForAI(GameTypeView gameTypeView, Session session, String gameName) {
        return createSimpleGameOptions(gameName, gameTypeView, session, PlayerType.COMPUTER_MAD);
    }

    private static GameTypeView prepareGameType(Session session) {
        GameTypeView gameType = session.getGameTypes()
                .stream()
                .filter(m -> m.getName().equals(TEST_AI_GAME_MODE))
                .findFirst()
                .orElse(null);
        Assert.assertNotNull("Can't find game type on the server: " + TEST_AI_GAME_MODE, gameType);
        return gameType;
    }
}
