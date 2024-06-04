package com.example.chess.firebase;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FirebaseUtils {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser currentUser;
    private StorageReference storageRef = FirebaseStorage.getInstance().getReference();

    private FirebaseDatabase database;
    private DatabaseReference activeUsersRef;

    public FirebaseUtils() {
        database = FirebaseDatabase.getInstance("https://chess-6d839-default-rtdb.europe-west1.firebasedatabase.app");
        activeUsersRef = database.getReference("active_users");
    }

    public interface UsernameCallback {
        void onUsernameFetched(String username);
    }


    public interface GameHistoryCallback {
        void onGameHistoryAdded(boolean isSuccessful);
    }

    public interface GameHistoryFetchCallback {
        void onGameHistoryFetched(List<Map<String, Object>> gameHistories);
    }
    public interface UpdateCredentialsCallback {
        void onUpdateCredentials(boolean isSuccessful);
    }
    public interface OnlineUsersCallback {
        void onOnlineUsersFetched(List<String> userIds);
    }
    public interface PgnFetchCallback {
        void onPgnFetched(List<String> pgnMoves);
    }
    public interface UserDetailsCallback {
        void onUserDetailsFetched(User user);
    }
    public interface OnRatingFetchedCallback {
        void onRatingFetched(Integer rating);

    }
    public interface AllUsersDetailsCallback {
        void onAllUsersDetailsFetched(List<User> users);
    }
    public interface OnImageUploadCallback {
        void onImageUpload(String imageUrl);
    }
    public interface ActiveUsersCallback {
        void onActiveUsersFetched(List<String> activeUserIds);
    }
    public interface getUidCallback {
        void onUidFetched(@Nullable String uid);
    }

    public void fetchUidForUsername(String username, getUidCallback callback) {
        db.collection("users").whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String uid = queryDocumentSnapshots.getDocuments().get(0).getId();
                        callback.onUidFetched(uid);
                    } else {
                        callback.onUidFetched(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("FirebaseUtils", "Error fetching UID for username", e);
                    callback.onUidFetched(null);
                });
    }

    public void addLocalGameHistory(FirebaseUser currentUser, Map<String, Object> gameHistoryData, GameHistoryCallback callback) {
        addGameHistoryToCollection(currentUser, "localGameHistory", gameHistoryData, callback);
    }

    public void addOnlineGameHistory(FirebaseUser currentUser, Map<String, Object> gameHistoryData, GameHistoryCallback callback) {
        addGameHistoryToCollection(currentUser, "onlineGameHistory", gameHistoryData, callback);
    }

    public void fetchLocalGameHistory(FirebaseUser currentUser, GameHistoryFetchCallback callback) {
        fetchGameHistoryFromCollection(currentUser, "localGameHistory", callback);
    }

    public void fetchOnlineGameHistory(FirebaseUser currentUser, GameHistoryFetchCallback callback) {
        fetchGameHistoryFromCollection(currentUser, "onlineGameHistory", callback);
    }
    public interface UpdateUserCallback {
        void onUpdateUser(boolean isSuccessful);
    }

    public void fetchUsername(FirebaseUser currentUser, UsernameCallback callback) {
        if (currentUser == null) {
            callback.onUsernameFetched("Sign In / Sign Up");
            return;
        }

        String userUID = currentUser.getUid();
        db.collection("users").document(userUID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String username = document.getString("username");
                    if (username != null) {
                        callback.onUsernameFetched(username);
                    } else {
                        callback.onUsernameFetched("Sign In");
                    }
                } else {
                    callback.onUsernameFetched("Sign In");
                }
            } else {
                callback.onUsernameFetched("Sign In");
            }
        });
    }

    public void fetchUserRating(FirebaseUser currentUser, OnRatingFetchedCallback callback) {
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Integer rating = documentSnapshot.getLong("rating").intValue();
                        callback.onRatingFetched(rating);
                    } else {
                        callback.onRatingFetched(null);
                    }
                })
                .addOnFailureListener(e -> callback.onRatingFetched(null));
    }
    public void fetchUserGames(FirebaseUser currentUser, OnRatingFetchedCallback callback) {
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Integer rating = documentSnapshot.getLong("games").intValue();
                        callback.onRatingFetched(rating);
                    } else {
                        callback.onRatingFetched(null);
                    }
                })
                .addOnFailureListener(e -> callback.onRatingFetched(null));
    }
    public void fetchUserDetails(FirebaseUser firebaseUser, UserDetailsCallback callback) {
        db.collection("users").document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onUserDetailsFetched(user);
                    } else {
                        Log.d("FirebaseUtils", "No such user document");
                    }
                })
                .addOnFailureListener(e -> Log.w("FirebaseUtils", "Error fetching user details", e));
    }

    private void addGameHistoryToCollection(FirebaseUser currentUser, String collectionName, Map<String, Object> gameHistoryData, GameHistoryCallback callback) {
        if (currentUser == null) {
            callback.onGameHistoryAdded(false);
            return;
        }

        String userUID = currentUser.getUid();

        db.collection("users")
                .document(userUID)
                .collection(collectionName)
                .add(gameHistoryData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onGameHistoryAdded(true);
                    } else {
                        callback.onGameHistoryAdded(false);
                    }
                });
    }

    private void fetchGameHistoryFromCollection(FirebaseUser currentUser, String collectionName, GameHistoryFetchCallback callback) {
        if (currentUser == null) {
            callback.onGameHistoryFetched(new ArrayList<>());
            return;
        }

        String userUID = currentUser.getUid();

        db.collection("users")
                .document(userUID)
                .collection(collectionName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Map<String, Object>> gameHistories = new ArrayList<>();
                        QuerySnapshot queryDocumentSnapshots = task.getResult();
                        if (queryDocumentSnapshots != null) {
                            for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                                gameHistories.add(snapshot.getData());
                            }
                        }
                        callback.onGameHistoryFetched(gameHistories);
                    } else {
                        callback.onGameHistoryFetched(new ArrayList<>());
                    }
                });
    }
    public void fetchPgnMovesFromGameHistory(FirebaseUser currentUser, String collectionName, PgnFetchCallback callback) {
        if (currentUser == null) {
            callback.onPgnFetched(new ArrayList<>());
            return;
        }

        String userUID = currentUser.getUid();

        db.collection("users")
                .document(userUID)
                .collection(collectionName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> pgnMovesList = new ArrayList<>();
                        QuerySnapshot queryDocumentSnapshots = task.getResult();
                        if (queryDocumentSnapshots != null) {
                            for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                                List<String> pgnMoves = (List<String>) snapshot.get("pgnMoves");
                                if (pgnMoves != null) {
                                    pgnMovesList.addAll(pgnMoves);
                                }
                            }
                        }
                        callback.onPgnFetched(pgnMovesList);
                    } else {
                        callback.onPgnFetched(new ArrayList<>());
                    }
                });
    }
    public void updateCredentials(FirebaseUser currentUser, Map<String, Object> newCredentials, UpdateCredentialsCallback callback) {
        if (currentUser == null) {
            callback.onUpdateCredentials(false);
            return;
        }

        String userUID = currentUser.getUid();

        db.collection("users")
                .document(userUID)
                .update(newCredentials)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onUpdateCredentials(true);
                    } else {
                        callback.onUpdateCredentials(false);
                    }
                });

    }
    public void uploadImageToFirebaseStorage(Bitmap imageBitmap, OnImageUploadCallback callback) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            callback.onImageUpload(null);
            return;
        }

        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        String userUID = firebaseUser.getUid();
        StorageReference userImageRef = storageRef.child("profileImages/" + userUID + ".jpg");

        UploadTask uploadTask = userImageRef.putBytes(data);
        uploadTask.addOnFailureListener(exception -> {
            // Handle unsuccessful uploads
            callback.onImageUpload(null);
        }).addOnSuccessListener(taskSnapshot -> {
            // Task completed successfully
            taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                callback.onImageUpload(imageUrl);
            });
        });
    }


    public void fetchActiveUsers(ActiveUsersCallback callback) {
        activeUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> activeUserIds = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    activeUserIds.add(userSnapshot.getKey());
                }
                callback.onActiveUsersFetched(activeUserIds);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onActiveUsersFetched(new ArrayList<>());
            }
        });
    }

    public void fetchAllUsersDetails(AllUsersDetailsCallback callback) {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<User> users = new ArrayList<>();
                QuerySnapshot queryDocumentSnapshots = task.getResult();
                if (queryDocumentSnapshots != null) {
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        User user = snapshot.toObject(User.class);
                        users.add(user);
                    }
                }
                callback.onAllUsersDetailsFetched(users);
            } else {
                Log.w("FirebaseUtils", "Error fetching all user details", task.getException());
            }
        });
    }

    public void addToActivePlayers(String username) {

        // Add the user's username to the active users list
        activeUsersRef.child(username).setValue(true);
    }


    public void removeFromActivePlayers(String username) {
        // Add the user's username to the active users list
        activeUsersRef.child(username).removeValue();
    }
    public interface UsernameExistsCallback {
        void onUsernameChecked(boolean exists);
    }

    public void checkUsernameExists(String username, UsernameExistsCallback callback) {
        db.collection("users").whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // If the query is successful and not empty, the username exists
                        boolean exists = !task.getResult().isEmpty();
                        callback.onUsernameChecked(exists);
                    } else {
                        // On failure, log the error and consider the username as not existing for safety
                        Log.w("FirebaseUtils", "Error checking if username exists", task.getException());
                        callback.onUsernameChecked(false);
                    }
                });
    }

}
