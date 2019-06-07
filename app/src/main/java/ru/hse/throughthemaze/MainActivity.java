package ru.hse.throughthemaze;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.LinearLayout;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.*;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.*;
import com.google.android.gms.tasks.*;
import ru.hse.throughthemaze.database.MapDBHandler;
import ru.hse.throughthemaze.database.MapDBManager;
import ru.hse.throughthemaze.gameplay.Ball;
import ru.hse.throughthemaze.gameplay.Balls;
import ru.hse.throughthemaze.gameplay.PhysicsEngine;
import ru.hse.throughthemaze.map.Map;
import ru.hse.throughthemaze.view.Draw2D;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = "Through the maze";

    // Request codes for the UIs that we show with startActivityForResult:
    private final static int RC_WAITING_ROOM = 10002;

    // Request code used to invoke sign in user interactions.
    private static final int RC_SIGN_IN = 9001;

    // Client used to sign in with Google APIs
    private GoogleSignInClient mGoogleSignInClient = null;

    // Client used to interact with the real time multiplayer system.
    private RealTimeMultiplayerClient mRealTimeMultiplayerClient = null;

    // Room ID where the currently active game is taking place; null if we're
    // not playing.
    private String mRoomId = null;

    // Holds the configuration of the current room.
    private RoomConfig mRoomConfig;

    // Are we playing in multiplayer mode?
    private boolean mMultiplayer = false;

    // The participants in the currently active game
    private ArrayList<Participant> mParticipants = null;

    // My participant ID in the currently active game
    private String mMyId = null;

    private String mHostId = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Create the client used to sign in.
        mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);

        // set up a click listener for everything we care about
        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }

        switchToMainScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        // Since the state of the signed in user can change when the activity is not active
        // it is recommended to try and sign in silently from when the app resumes.
        signInSilently();

        registerReceiver(accelerometerReceiver, new IntentFilter(Service.SENSOR_SERVICE));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_sign_in:
                // start the sign-in flow
                Log.d(TAG, "Sign-in button clicked");
                startSignInIntent();
                break;
            case R.id.button_sign_out:
                // user wants to sign out
                // sign out.
                Log.d(TAG, "Sign-out button clicked");
                signOut();
                switchToScreen(R.id.screen_sign_in);
                break;

            case R.id.button_multiplayer:
                // user wants to play against a random opponent right now
                startQuickGame();
                break;
        }
    }

    private void startQuickGame() {
        // quick-start a game with 1 randomly selected opponent
        final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 3;
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS,
                MAX_OPPONENTS, 0);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();

        mRoomConfig = RoomConfig.builder(mRoomUpdateCallback)
                .setOnMessageReceivedListener(mOnRealTimeMessageReceivedListener)
                .setRoomStatusUpdateCallback(mRoomStatusUpdateCallback)
                .setAutoMatchCriteria(autoMatchCriteria)
                .build();
        mRealTimeMultiplayerClient.create(mRoomConfig);
    }

    /**
     * Start a sign in activity.  To properly handle the result, call tryHandleSignInResult from
     * your Activity's onActivityResult function
     */
    private void startSignInIntent() {
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    /**
     * Try to sign in without displaying dialogs to the user.
     * <p>
     * If the user has already signed in previously, it will not show dialog.
     */
    private void signInSilently() {
        Log.d(TAG, "signInSilently()");

        mGoogleSignInClient.silentSignIn().addOnCompleteListener(this,
                new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInSilently(): success");
                            onConnected(task.getResult());
                        } else {
                            Log.d(TAG, "signInSilently(): failure", task.getException());
                            onDisconnected();
                        }
                    }
                });
    }

    private void signOut() {
        Log.d(TAG, "signOut()");

        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()) {
                            Log.d(TAG, "signOut(): success");
                        } else {
                            handleException(task.getException(), "signOut() failed!");
                        }

                        onDisconnected();
                    }
                });
    }

    /**
     * Since a lot of the operations use tasks, we can use a common handler for whenever one fails.
     *
     * @param exception The exception to evaluate.  Will try to display a more descriptive reason for the exception.
     * @param details   Will display alongside the exception if you wish to provide more details for why the exception
     *                  happened
     */
    private void handleException(Exception exception, String details) {
        int status = 0;

        if (exception instanceof ApiException) {
            ApiException apiException = (ApiException) exception;
            status = apiException.getStatusCode();
        }

        String errorString = null;
        switch (status) {
            case GamesCallbackStatusCodes.OK:
                break;
            case GamesClientStatusCodes.MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
                errorString = getString(R.string.status_multiplayer_error_not_trusted_tester);
                break;
            case GamesClientStatusCodes.MATCH_ERROR_ALREADY_REMATCHED:
                errorString = getString(R.string.match_error_already_rematched);
                break;
            case GamesClientStatusCodes.NETWORK_ERROR_OPERATION_FAILED:
                errorString = getString(R.string.network_error_operation_failed);
                break;
            case GamesClientStatusCodes.INTERNAL_ERROR:
                errorString = getString(R.string.internal_error);
                break;
            case GamesClientStatusCodes.MATCH_ERROR_INACTIVE_MATCH:
                errorString = getString(R.string.match_error_inactive_match);
                break;
            case GamesClientStatusCodes.MATCH_ERROR_LOCALLY_MODIFIED:
                errorString = getString(R.string.match_error_locally_modified);
                break;
            default:
                errorString = getString(R.string.unexpected_status, GamesClientStatusCodes.getStatusCodeString(status));
                break;
        }

        if (errorString == null) {
            return;
        }

        String message = getString(R.string.status_exception_error, details, status, exception);

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Error")
                .setMessage(message + "\n" + errorString)
                .setNeutralButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(intent);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                onConnected(account);
            } catch (ApiException apiException) {
                String message = apiException.getMessage();
                if (message == null || message.isEmpty()) {
                    message = getString(R.string.signin_other_error);
                }

                onDisconnected();

                new AlertDialog.Builder(this)
                        .setMessage(message)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        } else if (requestCode == RC_WAITING_ROOM) {
            // we got the result from the "waiting room" UI.
            if (resultCode == Activity.RESULT_OK) {
                // ready to start playing
                Log.d(TAG, "Starting game (waiting room returned OK).");
                startGame(true);
            } else if (resultCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                // player indicated that they want to leave the room
                leaveRoom();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // Dialog was cancelled (user pressed back key, for instance). In our game,
                // this means leaving the room too. In more elaborate games, this could mean
                // something else (like minimizing the waiting room UI).
                leaveRoom();
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(accelerometerReceiver);

        super.onPause();
    }

    // Activity is going to the background. We have to leave the current room.
    @Override
    public void onStop() {
        Log.d(TAG, "**** got onStop");

        // if we're in a room, leave it.
        leaveRoom();

        // stop trying to keep the screen on
        stopKeepingScreenOn();

        switchToMainScreen();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (bound) {
            bound = false;
            unbindService(connection);
        }

        super.onDestroy();
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
    private void leaveRoom() {
        Log.d(TAG, "Leaving room.");
        stopKeepingScreenOn();
        if (bound) {
            bound = false;
            unbindService(connection);
        }
        winner = -1;
        if (mRoomId != null) {
            mRealTimeMultiplayerClient.leave(mRoomConfig, mRoomId)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            mRoomId = null;
                            mRoomConfig = null;
                        }
                    });
            switchToScreen(R.id.screen_wait);
        } else {
            switchToMainScreen();
        }
    }

    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    private void showWaitingRoom(Room room) {
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = 2;
        mRealTimeMultiplayerClient.getWaitingRoomIntent(room, MIN_PLAYERS)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        // show waiting room UI
                        startActivityForResult(intent, RC_WAITING_ROOM);
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem getting the waiting room!"));
    }


    /*
     * CALLBACKS SECTION. This section shows how we implement the several games
     * API callbacks.
     */

    private String mPlayerId;

    // The currently signed in account, used to check the account has changed outside of this activity when resuming.
    private GoogleSignInAccount mSignedInAccount = null;

    private void onConnected(GoogleSignInAccount googleSignInAccount) {
        Log.d(TAG, "onConnected(): connected to Google APIs");
        if (mSignedInAccount != googleSignInAccount) {

            mSignedInAccount = googleSignInAccount;

            // update the clients
            mRealTimeMultiplayerClient = Games.getRealTimeMultiplayerClient(this, googleSignInAccount);
            // get the playerId from the PlayersClient
            PlayersClient playersClient = Games.getPlayersClient(this, googleSignInAccount);
            playersClient.getCurrentPlayer()
                    .addOnSuccessListener(new OnSuccessListener<Player>() {
                        @Override
                        public void onSuccess(Player player) {
                            mPlayerId = player.getPlayerId();

                            switchToMainScreen();
                        }
                    })
                    .addOnFailureListener(createFailureListener("There was a problem getting the player id!"));
        }

    }

    private OnFailureListener createFailureListener(final String string) {
        return new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                handleException(e, string);
            }
        };
    }

    private void onDisconnected() {
        Log.d(TAG, "onDisconnected()");

        mRealTimeMultiplayerClient = null;

        switchToMainScreen();
    }

    private RoomStatusUpdateCallback mRoomStatusUpdateCallback = new RoomStatusUpdateCallback() {
        // Called when we are connected to the room. We're not ready to play yet! (maybe not everybody
        // is connected yet).
        @Override
        public void onConnectedToRoom(Room room) {
            Log.d(TAG, "onConnectedToRoom.");

            //get participants and my ID:
            mParticipants = room.getParticipants();
            mMyId = room.getParticipantId(mPlayerId);

            // save room ID if its not initialized in onRoomCreated() so we can leave cleanly before the game starts.
            if (mRoomId == null) {
                mRoomId = room.getRoomId();
            }

            // print out the list of participants (for debug purposes)
            Log.d(TAG, "Room ID: " + mRoomId);
            Log.d(TAG, "My ID " + mMyId);
            Log.d(TAG, "<< CONNECTED TO ROOM>>");
        }

        // Called when we get disconnected from the room. We return to the main screen.
        @Override
        public void onDisconnectedFromRoom(Room room) {
            mRoomId = null;
            mRoomConfig = null;
            showGameError();
        }


        // We treat most of the room update callbacks in the same way: we update our list of
        // participants and update the display. In a real game we would also have to check if that
        // change requires some action like removing the corresponding player avatar from the screen,
        // etc.
        @Override
        public void onPeerDeclined(Room room, @NonNull List<String> arg1) {
            updateRoom(room);
        }

        @Override
        public void onPeerInvitedToRoom(Room room, @NonNull List<String> arg1) {
            updateRoom(room);
        }

        @Override
        public void onP2PDisconnected(@NonNull String participant) {
        }

        @Override
        public void onP2PConnected(@NonNull String participant) {
        }

        @Override
        public void onPeerJoined(Room room, @NonNull List<String> arg1) {
            updateRoom(room);
        }

        @Override
        public void onPeerLeft(Room room, @NonNull List<String> peersWhoLeft) {
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
        public void onPeersConnected(Room room, @NonNull List<String> peers) {
            updateRoom(room);
        }

        @Override
        public void onPeersDisconnected(Room room, @NonNull List<String> peers) {
            updateRoom(room);
        }
    };

    // Show error message about game being cancelled and return to main screen.
    private void showGameError() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.game_problem))
                .setNeutralButton(android.R.string.ok, null).create();

        switchToMainScreen();
    }

    private RoomUpdateCallback mRoomUpdateCallback = new RoomUpdateCallback() {

        // Called when room has been created
        @Override
        public void onRoomCreated(int statusCode, Room room) {
            Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
            if (statusCode != GamesCallbackStatusCodes.OK) {
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
            if (statusCode != GamesCallbackStatusCodes.OK) {
                Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
                showGameError();
                return;
            }
            updateRoom(room);
            mHostId = mParticipants.get(0).getParticipantId();
        }

        @Override
        public void onJoinedRoom(int statusCode, Room room) {
            Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
            if (statusCode != GamesCallbackStatusCodes.OK) {
                Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
                showGameError();
                return;
            }

            // show the waiting room UI
            showWaitingRoom(room);
        }

        // Called when we've successfully left the room (this happens a result of voluntarily leaving
        // via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).
        @Override
        public void onLeftRoom(int statusCode, @NonNull String roomId) {
            // we have left the room; return to main screen.
            Log.d(TAG, "onLeftRoom, code " + statusCode);
            switchToMainScreen();
        }
    };

    private void updateRoom(Room room) {
        if (room != null) {
            if (mCurScreen == R.id.screen_game && mParticipants.size() > room.getParticipants().size()) {
                winner = -2;
                leaveRoom();
            }
            mParticipants = room.getParticipants();
        }
    }

    private RealTimeMultiplayerClient.ReliableMessageSentCallback reliable = new RealTimeMultiplayerClient.ReliableMessageSentCallback() {
        @Override
        public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientParticipantId) {
            Log.d(TAG, "RealTime message sent");
            Log.d(TAG, "  statusCode: " + statusCode);
            Log.d(TAG, "  tokenId: " + tokenId);
            Log.d(TAG, "  recipientParticipantId: " + recipientParticipantId);
        }
    };

    /*
     * GAME LOGIC SECTION. Methods that implement the game's rules.
     */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PhysicsEngine.EngineBinder binder = (PhysicsEngine.EngineBinder) service;
            engine = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };



    public static final long UPDATE_FREQUENCY = 20;
    private PhysicsEngine engine;
    private SQLiteDatabase db;
    private MapDBManager manager;
    private volatile boolean bound;
    private Intent accelerometer;
    private Draw2D draw;
    private volatile int mapId;
    private Map map;
    private int notReady;
    private int curStage;
    private volatile int arrayIndex;
    private Ball[] balls;
    private volatile int winner;

    private void resetVars() {
        engine = null;
        db = null;
        manager = null;
        bound = false;
        accelerometer = null;
        draw = null;
        mapId = -1;
        map = null;
        curStage = 0;
        arrayIndex = -1;
        balls = null;
        winner = -1;
    }

    private BroadcastReceiver accelerometerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (balls == null) {
                return;
            }
            Ball ball = intent.getParcelableExtra(Ball.class.getName());
            synchronized (balls[arrayIndex]) {
                balls[arrayIndex].ax = ball.ax;
                balls[arrayIndex].ay = ball.ay;
            }
        }
    };


    // Start the gameplay phase of the game.
    private void startGame(boolean multiplayer) {
        resetVars();
        int index = 0;
        for (Participant p: mParticipants) {
            if (p.getParticipantId().equals(mMyId)) {
                arrayIndex = index;
            }
            index++;
        }

        curStage = 0;

        gameStage(0);
    }

    private void gameStage(int stage) {
        if (stage == 0) {
            ((LinearLayout)findViewById(R.id.screen_game)).removeAllViews();
            switchToScreen(R.id.screen_game);
            if (mHostId.equals(mMyId)) {
                MapDBHandler dbLoader = new MapDBHandler(this);
                db = dbLoader.getReadableDatabase();
                manager = new MapDBManager(db);
                int mapCount = manager.getMapCount();
                Random random = new Random();
                mapId = random.nextInt(mapCount);

                ByteBuffer buffer = ByteBuffer.allocate(4).putInt(mapId);
                buffer.flip();
                byte[] bytes = new byte[4];
                buffer.get(bytes);

                sendReliableMessageToOthers(bytes);
                gameStage(++curStage);
            }
        } else if (stage == 1) {
            if (!mHostId.equals(mMyId)) {
                MapDBHandler dbLoader = new MapDBHandler(this);
                db = dbLoader.getReadableDatabase();
                manager = new MapDBManager(db);
            }
            map = manager.loadMap(mapId);
            db.close();
            map.start = new int[mParticipants.size()];
            if (mHostId.equals(mMyId)) {
                map.pickStartAndEnd(mParticipants.size());

                notReady = mParticipants.size() - 1;

                ByteBuffer buffer = ByteBuffer.allocate((mParticipants.size() + 1) * 4);
                for (int i = 0; i < mParticipants.size(); i++) {
                    buffer.putInt(map.start[i]);
                }
                buffer.putInt(map.end);

                buffer.flip();

                byte[] array = new byte[buffer.remaining()];
                buffer.get(array);
                sendReliableMessageToOthers(array);

                balls = new Ball[map.start.length];
                for (int i = 0; i < map.start.length; i++) {
                    balls[i] = new Ball(map.vertexes[map.start[i]].x, map.vertexes[map.start[i]].y);
                }
            }
        } else if (stage == 2) {
            if (mHostId.equals(mMyId)) {
                GameCycleThread server = new GameCycleThread();
                server.start();
            }
            accelerometer = new Intent(this, AccelerometerService.class);
            accelerometer.putExtra(Ball.class.getName(), balls[arrayIndex]);
            startService(accelerometer);
            ViewThread view = new ViewThread();
            view.start();
        }
    }

    class ViewThread implements Runnable {

        private void start() {
            draw = new Draw2D(MainActivity.this);
            draw.map = map;
            draw.balls = balls;
            draw.index = arrayIndex;
            ((LinearLayout)findViewById(R.id.screen_game)).addView(draw);
            Thread worker = new Thread(this);
            worker.start();
        }

        @Override
        public void run() {
            while (winner == -1) {
                long time = System.currentTimeMillis();

                if (draw == null) {
                    break;
                }

                for (int i = 0; i < balls.length; i++) {
                    synchronized (balls[i]) {
                        draw.balls[i] = new Ball(balls[i]);
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (draw != null) {
                            draw.invalidate();
                        }
                    }
                });

                if (!mHostId.equals(mMyId)) {
                    byte[] bytes = new byte[Ball.SIZE + 4];
                    ByteBuffer buffer = ByteBuffer.allocate(Ball.SIZE + 4).putInt(arrayIndex);
                    byte[] ballBytes;
                    synchronized (balls[arrayIndex]) {
                        ballBytes = balls[arrayIndex].write();
                    }
                    buffer.put(ballBytes);
                    buffer.flip();
                    buffer.get(bytes);
                    mRealTimeMultiplayerClient.sendUnreliableMessage(bytes, mRoomId, mHostId);
                }

                long cycleTime = System.currentTimeMillis() - time;
                if (cycleTime < UPDATE_FREQUENCY) {
                    try {
                        Thread.sleep(UPDATE_FREQUENCY - cycleTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    class GameCycleThread implements Runnable {

        private void start() {
            Thread worker = new Thread(this);
            worker.start();
        }

        @Override
        public void run() {
            while (winner == -1) {
                long time = System.currentTimeMillis();

                if (bound) {
                    Ball[] ballsEngine = new Ball[mParticipants.size()];
                    for (int i = 0; i < mParticipants.size(); i++) {
                        Ball ball = engine.getBall(i);
                        if (ball.color == -1) {
                            winner = i;

                            byte[] array = new byte[4];
                            ByteBuffer buffer = ByteBuffer.allocate(4).putInt(i);
                            buffer.flip();
                            buffer.get(array);
                            sendReliableMessageToOthers(array);
                            return;
                        }
                        ballsEngine[i] = ball;
                    }

                    mRealTimeMultiplayerClient.sendUnreliableMessageToOthers(Ball.toByteArray(ballsEngine), mRoomId);

                    synchronized (balls[arrayIndex]) {
                        double ax = balls[arrayIndex].ax;
                        double ay = balls[arrayIndex].ay;
                        balls = ballsEngine;
                        balls[arrayIndex].ax = ax;
                        balls[arrayIndex].ay = ay;
                        engine.updateBall(arrayIndex, balls[arrayIndex]);
                    }

                }

                long cycleTime = System.currentTimeMillis() - time;
                if (cycleTime < UPDATE_FREQUENCY) {
                    try {
                        Thread.sleep(UPDATE_FREQUENCY - cycleTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /*
     * COMMUNICATIONS SECTION. Methods that implement the game's network
     * protocol.
     */


    // Called when we receive a real-time message from the network.
    // Messages in our game are made up of 2 bytes: the first one is 'F' or 'U'
    // indicating
    // whether it's a final or interim score. The second byte is the score.
    // There is also the
    // 'S' message, which indicates that the game should start.
    private OnRealTimeMessageReceivedListener mOnRealTimeMessageReceivedListener = new OnRealTimeMessageReceivedListener() {
        @Override
        public void onRealTimeMessageReceived(@NonNull RealTimeMessage realTimeMessage) {
            if (winner != -1) {
                return;
            }
            ByteBuffer buffer = ByteBuffer.allocate(realTimeMessage.getMessageData().length);
            buffer.put(realTimeMessage.getMessageData());
            buffer.flip();
            if (realTimeMessage.isReliable()) {
                if (curStage == 0) {
                    mapId = buffer.getInt();
                    gameStage(++curStage);
                } else if (curStage == 1) {
                    if (!mHostId.equals(mMyId)) {
                        if (buffer.remaining() > 1) {
                            balls = new Ball[mParticipants.size()];
                            for (int i = 0; i < map.start.length; i++) {
                                map.start[i] = buffer.getInt();
                                balls[i] = new Ball(map.vertexes[map.start[i]].x, map.vertexes[map.start[i]].y);
                            }
                            map.end = buffer.getInt();
                            byte[] bytes = new byte[1];
                            mRealTimeMultiplayerClient.sendReliableMessage(bytes, mRoomId, mHostId, reliable);
                        } else {
                            gameStage(++curStage);
                        }
                    } else {
                        notReady--;
                        if (notReady == 0) {
                            PhysicsEngine.map = map;
                            Intent intent = new Intent(MainActivity.this, PhysicsEngine.class);
                            intent.putExtra(Balls.class.getName(), new Balls(balls));
                            bindService(intent, connection, Context.BIND_AUTO_CREATE);
                            byte[] bytes = new byte[1];
                            sendReliableMessageToOthers(bytes);
                            gameStage(++curStage);
                        }
                    }
                } else if (curStage == 2) {
                    if (mHostId.equals(mMyId)) {
                        if (bound) {
                            bound = false;
                            unbindService(connection);
                        }
                    }
                    stopService(accelerometer);
                    winner = buffer.getInt();
                }
            } else {
                if (!mHostId.equals(mMyId)) {
                    synchronized (balls[arrayIndex]) {
                        double ax = balls[arrayIndex].ax;
                        double ay = balls[arrayIndex].ay;
                        balls = Ball.fromByteArray(realTimeMessage.getMessageData());
                        balls[arrayIndex].ax = ax;
                        balls[arrayIndex].ay = ay;
                    }
                } else {
                    int index = buffer.getInt();
                    Ball ball = new Ball();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    ball.read(bytes);
                    if (engine != null) {
                        engine.updateBall(index, ball);
                    }
                }
            }
        }
    };

    private void sendReliableMessageToOthers(byte[] bytes) {
        for (Participant p: mParticipants) {
            if (p.getStatus() == Participant.STATUS_JOINED && !p.getParticipantId().equals(mMyId)) {
                mRealTimeMultiplayerClient.sendReliableMessage(bytes, mRoomId, p.getParticipantId(), reliable);
            }
        }
    }

    /*
     * UI SECTION. Methods that implement the game's UI.
     */

    // This array lists everything that's clickable, so we can install click
    // event handlers.
    private final static int[] CLICKABLES = {
            R.id.button_multiplayer,
            R.id.button_sign_in,
            R.id.button_sign_out
    };

    // This array lists all the individual screens our game has.
    private final static int[] SCREENS = {
            R.id.screen_game, R.id.screen_main, R.id.screen_sign_in,
            R.id.screen_wait
    };
    private int mCurScreen = -1;

    private void switchToScreen(int screenId) {
        // make the requested screen visible; hide all others.
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
        mCurScreen = screenId;
    }

    private void switchToMainScreen() {
        if (mRealTimeMultiplayerClient != null) {
            switchToScreen(R.id.screen_main);
        } else {
            switchToScreen(R.id.screen_sign_in);
        }
    }

    /*
     * MISC SECTION. Miscellaneous methods.
     */


    // Sets the flag to keep this screen on. It's recommended to do that during
    // the
    // handshake when setting up a game, because if the screen turns off, the
    // game will be
    // cancelled.
    private void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    private void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
