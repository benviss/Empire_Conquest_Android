package com.vizo.empireconquest.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.example.games.basegameutils.BaseGameUtils;
import com.vizo.empireconquest.R;
import com.vizo.empireconquest.models.Board;
import com.vizo.empireconquest.models.Player;
import com.vizo.empireconquest.models.Territory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GameActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, RealTimeMessageReceivedListener,
        RoomStatusUpdateListener, RoomUpdateListener, Serializable{

    final static String TAG = "Ben Vissotzky";

    // Request codes for the UIs that we show with startActivityForResult:
    final static int RC_WAITING_ROOM = 10002;

    // Request code used to invoke sign in user interactions.
    private static final int RC_SIGN_IN = 9001;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Set to true to automatically start the sign in flow when the Activity starts.
    // Set to false to require the user to click the button in order to sign in.
    private boolean mAutoStartSignInFlow = true;

    // Room ID where the currently active game is taking place; null if we're
    // not playing.
    String mRoomId = null;

    // The participants in the currently active game
    ArrayList<Participant> mParticipants = null;

    // My participant ID in the currently active game
    String mMyId = null;

    // Message buffer for sending messages
    byte[] mMsgBuf = new byte[2];

    // Board object
    Board board;

    // Player Array
    ArrayList<Player> players = new ArrayList<>();

    //Keeping track of player inputs, determining next round or not


    //Territory Array
    ArrayList<Territory> territories = new ArrayList<>();

    //Game State
    boolean gameOn = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_map);
        switchToScreen(R.id.screen_wait);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        mGoogleApiClient.connect();

        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_alaska:
                for (Territory t : territories) {
                    Log.d(TAG, t.getName() + "||" + t.getPlayerOwned().getName());
                }
                break;
        }
    }

    void startQuickGame() {
        //quick game with 1 random opponent
        final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS, MAX_OPPONENTS, 0);
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        keepScreenOn();
        resetGameVars();
        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode,
                                 Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);

        switch (requestCode) {
            case RC_WAITING_ROOM:
                // we got the result from the "waiting room" UI.
                if (responseCode == Activity.RESULT_OK) {
                    // ready to start playing
                    Log.d(TAG, "Starting game (waiting room returned OK).");
                    startGame();
                } else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    // player indicated that they want to leave the room
                    leaveRoom();
                } else if (responseCode == Activity.RESULT_CANCELED) {
                    // Dialog was cancelled (user pressed back key, for instance). In our game,
                    // this means leaving the room too. In more elaborate games, this could mean
                    // something else (like minimizing the waiting room UI).
                    leaveRoom();
                }
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
    }


    // Activity just got to the foreground. We switch to the wait screen because we will now
    // go through the sign-in flow (remember that, yes, every time the Activity comes back to the
    // foreground we go through the sign-in flow -- but if the user is already authenticated,
    // this flow simply succeeds and is imperceptible).
    @Override
    public void onStart() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.w(TAG,
                    "GameHelper: client was already connected on onStart()");
        } else {
            Log.d(TAG, "Connecting client.");
            mGoogleApiClient.connect();
        }
        super.onStart();
    }

    // Activity is going to the background. We have to leave the current room.
    @Override
    public void onStop() {
        Log.d(TAG, "**** got onStop");

        // if we're in a room, leave it.
        leaveRoom();

        // stop trying to keep the screen on
        stopKeepingScreenOn();

        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
//            switchToScreen(R.id.screen_sign_in);
        } else {
            switchToScreen(R.id.screen_wait);
        }
        super.onStop();
    }

    // Handle back key to make sure we cleanly leave a game if we are in the middle of one
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mCurScreen == R.id.screen_game) {
            leaveRoom();
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    // Leave the room.
    void leaveRoom() {
        Log.d(TAG, "Leaving room.");
        stopKeepingScreenOn();
        if (mRoomId != null) {
            Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoomId);
            mRoomId = null;
            switchToScreen(R.id.screen_wait);
        } else {
//            switchToMainScreen();
        }
    }

    /*
     * CALLBACKS SECTION. This section shows how we implement the several games
     * API callbacks.
     */

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected() called. Sign in successful!");

        Log.d(TAG, "Sign-in succeeded.");

        // register listener so we are notified if we receive an invitation to play
        // while we are in the game
//        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);

        if (connectionHint != null) {
            Log.d(TAG, "onConnected: connection hint provided. Checking for invite.");
            Invitation inv = connectionHint
                    .getParcelable(Multiplayer.EXTRA_INVITATION);
            if (inv != null && inv.getInvitationId() != null) {
                // retrieve and cache the invitation ID
                Log.d(TAG, "onConnected: connection hint has a room invite!");
//                acceptInviteToRoom(inv.getInvitationId());
                return;
            }
        }
        startQuickGame();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() called. Trying to reconnect.");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed() called, result: " + connectionResult);

        if (mResolvingConnectionFailure) {
            Log.d(TAG, "onConnectionFailed() ignoring connection failure; already resolving.");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient,
                    connectionResult, RC_SIGN_IN, "Ruh Roh");
        }

//        switchToScreen(R.id.screen_sign_in);
    }

    // Called when we are connected to the room. We're not ready to play yet! (maybe not everybody
    // is connected yet).
    @Override
    public void onConnectedToRoom(Room room) {
        Log.d(TAG, "onConnectedToRoom.");

        //get participants and my ID:
        mParticipants = room.getParticipants();
        for (Participant p : mParticipants) {
            Player newPlayer = new Player(p.getDisplayName(), p.getParticipantId());
            players.add(newPlayer);
        }
        mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));

        // save room ID if its not initialized in onRoomCreated() so we can leave cleanly before the game starts.
        if (mRoomId == null)
            mRoomId = room.getRoomId();

        // print out the list of participants (for debug purposes)
        Log.d(TAG, "Room ID: " + mRoomId);
        Log.d(TAG, "My ID " + mMyId);
        Log.d(TAG, "<< CONNECTED TO ROOM>>");
    }

    @Override
    public void onLeftRoom(int statusCode, String roomId) {
        // we have left the room; return to main screen.
        Log.d(TAG, "onLeftRoom, code " + statusCode);
        switchToMainScreen();
    }


    // Called when we get disconnected from the room. We return to the main screen.
    @Override
    public void onDisconnectedFromRoom(Room room) {
        mRoomId = null;
        showGameError();
    }

    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    void showWaitingRoom(Room room) {
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);

        // show waiting room UI
        startActivityForResult(i, RC_WAITING_ROOM);
    }

    // Called when room has been created
    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
            showGameError();
            return;
        }

        // save room ID so we can leave cleanly before the game starts.
        mRoomId = room.getRoomId();

        // show the waiting room UI
        showWaitingRoom(room);
    }

    // Called when room is fully connected.
    @Override
    public void onRoomConnected(int statusCode, Room room) {
        Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }
        updateRoom(room);
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }

        // show the waiting room UI
        showWaitingRoom(room);
    }

    void showGameError() {
        BaseGameUtils.makeSimpleDialog(this, "Game problem?");
        switchToMainScreen();
    }

    // We treat most of the room update callbacks in the same way: we update our list of
    // participants and update the display. In a real game we would also have to check if that
    // change requires some action like removing the corresponding player avatar from the screen,
    // etc.
    @Override
    public void onPeerDeclined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onP2PDisconnected(String participant) {
    }

    @Override
    public void onP2PConnected(String participant) {
    }

    @Override
    public void onPeerJoined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, List<String> peersWhoLeft) {
        updateRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        updateRoom(room);
    }

    @Override
    public void onRoomConnecting(Room room) {
        updateRoom(room);
    }

    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        updateRoom(room);
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        updateRoom(room);
    }


    void updateRoom(Room room) {
        if (room != null) {
            mParticipants = room.getParticipants();
        }
        if (mParticipants != null) {

        }
    }

    /*
     * GAME LOGIC SECTION. Methods that implement the game's rules.
     */
    public void run()
    {
        //Looping until the boolean is false
//        while (gameOn)
//        {
////            processInput();
//            update();
//            try {Thread.sleep(1000);}
//            catch (InterruptedException ex) {}
//        }
    }

    // Reset game variables in preparation for a new game.
    void resetGameVars() {
        // Deletes room and stuff
    }

    // determines creator of board, initializes board sync and starts main game loop
    void startGame() {
        switchToScreen(R.id.screen_game);
        board = new Board();

        players = sortPlayers(players);
        if (players.get(0).getPlayerId() == mMyId) {
            createBoard(players);
            syncTerritories(board.getTerritories(), players);
        }
        gameOn = true;
        run();
    }

    //Sorts Players by id and determines creator of board
    public ArrayList<Player> sortPlayers(ArrayList<Player> players) {
        Collections.sort(players, new Comparator<Player>() {
            public int compare(Player p1, Player p2) {
                return p1.getName().compareTo(p2.getName());
            }
        });
        return players;
    }

    //Only called by creator of board. Determines random owners of territories
    public void createBoard(ArrayList<Player> players) {
        board.assignTerritories(players);
        territories = board.getTerritories();
    }

    public void update() {

    }

    /*
     * COMMUNICATIONS SECTION. Methods that implement the game's network
     * protocol.
     */

    // Called when we receive a real-time message from the network.
    // Message of 0 means board generation command received
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {
        byte[] buf = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();
        switch (buf[0]) {
            case 0:
                syncGameData(buf, players);
                break;
        }
//        Toast.makeText(GameActivity.this, "Message received: " + (int) buf[0] + "/" + (int) buf[1], Toast.LENGTH_SHORT).show();
    }

    public void syncGameData(byte[] buf, ArrayList<Player> players) {
        ArrayList<String> indexedTerritories = board.getIndexedTerritories();
        int t = 1;
        int p = 2;
        //TODO dont hardcode you noob
        for (int i = 1; i < 40; i++) {
            Territory newTerritory = new Territory(indexedTerritories.get(buf[t]), players.get(buf[p]));
            territories.add(newTerritory);
            t += 2;
            p += 2;
        }
    }

    //converts territory array into byte array to transmit over GPS API
    public void syncTerritories(ArrayList<Territory> territories, ArrayList<Player> players) {
        byte[] array = new byte[territories.size() * 2 + 1];
        //sets command of byte array to instruct message receiver
        array[0] = (byte) 0;
        ArrayList<String> indexedTerritories = board.getIndexedTerritories();
        //sets index to assign territory and player
        int tI = 1;
        int pI = 2;
        for (int i = 0; i < territories.size(); i++) {
            //associates territory object with its territory name in indexedTerritories
            int territory = indexedTerritories.indexOf(territories.get(i).getName());

            //associates player with index of player in shared players array
            int player = players.indexOf(territories.get(i).getPlayerOwned());

            //saves territory index and player index to byte[]
            array[tI] = (byte) territory;
            array[pI] = (byte) player;

            tI += 2;
            pI += 2;
        }
        for (Participant p : mParticipants) {
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, array, mRoomId, p.getParticipantId());
        }
    }

    /*
     * UI SECTION. Methods that implement the game's UI.
     */

    final static int[] CLICKABLES = {
            R.id.button_alaska, R.id.button_nwTerritory, R.id.button_greenland, R.id.button_alberta, R.id.button_ontario, R.id.button_quebec, R.id.button_westernUs, R.id.button_easternUs, R.id.button_centralAmerica
    };

    // This array lists all the individual screens our game has.
    final static int[] SCREENS = {
            R.id.screen_game,
            R.id.screen_wait
    };
    int mCurScreen = -1;

    void switchToScreen(int screenId) {
        //make the requested screen visible; hide all others
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
        mCurScreen = screenId;
    }

    // Sets the flag to keep this screen on. It's recommended to do that during
    // the
    // handshake when setting up a game, because if the screen turns off, the
    // game will be
    // cancelled.
    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    void switchToMainScreen() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
//            switchToScreen(R.id.screen_main);
            Log.d(TAG, "switchToMainScreen if");
            Intent intent = new Intent(GameActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            Log.d(TAG, "switchToMainScreen else");
            Intent intent = new Intent(GameActivity.this, MainActivity.class);
            startActivity(intent);
//            switchToScreen(R.id.screen_sign_in);
        }
    }
}
