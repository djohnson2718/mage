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
        logger.info("hey I entered the funciton at least!!!!!!");
        String gameName ="AI Duel";
        String deckColors = "UB";
        String deckAllowedSets ="M20";
        LoadTestGameResultsList rl = new LoadTestGameResultsList();
        LoadTestGameResult gameResult = rl.createGame(0, "ai_test", 1);

        
        Assert.assertFalse("need deck colors", deckColors.isEmpty());
        Assert.assertFalse("need allowed sets", deckAllowedSets.isEmpty());

        // monitor and game source
        LoadPlayer monitor = new LoadPlayer("m", true);

        // game by monitor
        GameTypeView gameType = prepareGameType(monitor.session);
        MatchOptions gameOptions = createSimpleGameOptionsForAI(gameType, monitor.session, gameName);
        TableView game = monitor.session.createTable(monitor.roomID, gameOptions);
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
        Assert.assertTrue(monitor.session.joinTable(monitor.roomID, tableId, "ai_1", PlayerType.COMPUTER_MAD, 5, deckList1, ""));
        Assert.assertTrue(monitor.session.joinTable(monitor.roomID, tableId, "ai_2", PlayerType.COMPUTER_MAD, 5, deckList2, ""));

        // match start
        Assert.assertTrue(monitor.session.startMatch(monitor.roomID, tableId));

        // playing until game over
        gameResult.start();
        boolean startToWatching = false;
        while (true) {
            GameView gameView = monitor.client.getLastGameView();

            checkGame = monitor.getTable(tableId);
            TableState state = checkGame.get().getTableState();

            logger.warn(checkGame.get().getTableName()
                    + (gameView != null ? ", turn " + gameView.getTurn() + ", " + gameView.getStep().toString() : "")
                    + (gameView != null ? ", active " + gameView.getActivePlayerName() : "")
                    + ", " + state);

            if (state == TableState.FINISHED) {
                gameResult.finish(gameView);
                break;
            }

            if (!startToWatching && state == TableState.DUELING) {
                Assert.assertTrue(monitor.session.watchGame(checkGame.get().getGames().iterator().next()));
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

        for (String deckType : session.getDeckTypes()){
            logger.info(deckType);
        }
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

    private static MatchOptions createSimpleGameOptionsForBots(GameTypeView gameTypeView, Session session) {
        return createSimpleGameOptions("Bots test game", gameTypeView, session, PlayerType.HUMAN);
    }

    private static MatchOptions createSimpleGameOptionsForAI(GameTypeView gameTypeView, Session session, String gameName) {
        return createSimpleGameOptions(gameName, gameTypeView, session, PlayerType.COMPUTER_MAD);
    }

    private static class LoadPlayer {

        String userName;
        Connection connection;
        SimpleMageClient client;
        Session session;
        UUID roomID;
        UUID createdTableID;
        UUID connectedTableID;
        DeckCardLists deckList;
        String lastGameResult = "";

        public LoadPlayer(String userPrefix) {
            this(userPrefix, false);
        }

        public LoadPlayer(String userPrefix, boolean joinGameChat) {
            this.userName = TEST_USER_NAME + "_" + userPrefix + "_" + RandomUtil.nextInt(10000);
            this.connection = createSimpleConnection(this.userName);
            this.client = new SimpleMageClient(joinGameChat);
            this.session = new SessionImpl(this.client);

            this.session.connect(this.connection);
            this.client.setSession(this.session);
            this.roomID = this.session.getMainRoomId();
        }

        public ArrayList<UsersView> getAllRoomUsers() {
            ArrayList<UsersView> res = new ArrayList<>();
            try {
                for (RoomUsersView roomUsers : this.session.getRoomUsers(this.roomID)) {
                    res.addAll(roomUsers.getUsersView());
                }
            } catch (MageRemoteException e) {
                logger.error(e);
            }
            return res;
        }



        public Optional<TableView> getTable(UUID tableID) {
            return this.session.getTable(this.roomID, tableID);
        }
      


    }

    private static class LoadTestGameResult {
        int index;
        String name;
        long randomSeed;
        Date timeStarted;
        Date timeEnded = null;
        GameView finalGameView = null;

        public LoadTestGameResult(int index, String name, long randomSeed) {
            this.index = index;
            this.name = name;
            this.randomSeed = randomSeed;
        }

        public void start() {
            this.timeStarted = new Date();
        }

        public void finish(GameView finalGameView) {
            this.timeEnded = new Date();
            this.finalGameView = finalGameView;
        }

        public int getLife1() {
            return this.finalGameView.getPlayers().get(0).getLife();
        }

        public int getLife2() {
            return this.finalGameView.getPlayers().get(1).getLife();
        }

        public int getTurn() {
            return this.finalGameView.getTurn();
        }

        public int getDuration() {
            return (int) ((this.timeEnded.getTime() - this.timeStarted.getTime()) / 1000);
        }
    }

    private static class LoadTestGameResultsList extends HashMap<Integer, LoadTestGameResult> {

        private static final String tableFormatHeader = "|%-10s|%-15s|%-20s|%-10s|%-15s|%-15s|%-10s|%-20s|%n";
        private static final String tableFormatData = "|%-10s|%15s|%20s|%10s|%15s|%15s|%10s|%20s|%n";

        public LoadTestGameResult createGame(int index, String name, long randomSeed) {
            if (this.containsKey(index)) {
                throw new IllegalArgumentException("Game with index " + index + " already exists");
            }
            LoadTestGameResult res = new LoadTestGameResult(index, name, randomSeed);
            this.put(index, res);
            return res;
        }

        public void printResultHeader() {
            List<String> data = Arrays.asList(
                    "index",
                    "name",
                    "random sid",
                    "turn",
                    "player 1",
                    "player 2",
                    "time, sec",
                    "time per turn, sec"
            );
            System.out.printf(tableFormatHeader, data.toArray());
        }

        public void printResultData() {
            this.values().forEach(this::printResultData);
        }

        public void printResultData(LoadTestGameResult gameResult) {
            List<String> data = Arrays.asList(
                    String.valueOf(gameResult.index), //"index",
                    gameResult.name, //"name",
                    String.valueOf(gameResult.randomSeed), // "random sid",
                    String.valueOf(gameResult.getTurn()), //"turn",
                    String.valueOf(gameResult.getLife1()), //"player 1",
                    String.valueOf(gameResult.getLife2()), //"player 2",
                    String.valueOf(gameResult.getDuration()),// "time, sec",
                    String.valueOf(gameResult.getDuration() / gameResult.getTurn()) //"per turn, sec"
            );
            System.out.printf(tableFormatData, data.toArray());
        }

        public void printResultTotal() {
            List<String> data = Arrays.asList(
                    "TOTAL/AVG", //"index",
                    String.valueOf(this.size()), //"name",
                    "", // "random sid",
                    String.valueOf(this.getAvgTurn()), // turn
                    String.valueOf(this.getAvgLife1()), // player 1
                    String.valueOf(this.getAvgLife2()), // player 2
                    String.valueOf(this.getAvgDuration()), // time, sec
                    String.valueOf(this.getAvgDurationPerTurn()) // time per turn, sec
            );
            System.out.printf(tableFormatData, data.toArray());
        }

        private int getAvgTurn() {
            return this.values().stream().mapToInt(LoadTestGameResult::getTurn).sum() / this.size();
        }

        private int getAvgLife1() {
            return this.values().stream().mapToInt(LoadTestGameResult::getLife1).sum() / this.size();
        }

        private int getAvgLife2() {
            return this.values().stream().mapToInt(LoadTestGameResult::getLife2).sum() / this.size();
        }

        private int getAvgDuration() {
            return this.values().stream().mapToInt(LoadTestGameResult::getDuration).sum() / this.size();
        }

        private int getAvgDurationPerTurn() {
            return getAvgDuration() / getAvgTurn();
        }
    }

    private static GameTypeView prepareGameType(Session session) {
        for (GameTypeView gameType : session.getGameTypes()) {
            logger.info(gameType.getName());
        }
        GameTypeView gameType = session.getGameTypes()
                .stream()
                .filter(m -> m.getName().equals(TEST_AI_GAME_MODE))
                .findFirst()
                .orElse(null);
        Assert.assertNotNull("Can't find game type on the server: " + TEST_AI_GAME_MODE, gameType);
        return gameType;
    }
}
