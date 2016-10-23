package jp.techacademy.kanta.nakayama.qa_app;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QuestionDetailActivity extends AppCompatActivity {
    private ListView mListView;
    private Question mQuestion;
    private QuestionDetailListAdapter mAdapter;

    private DatabaseReference mAnswerRef;

    //ひとまずテスト用のfavoriteButtonを作成します
    private Button favoriteButton;
    private ArrayList<String> favoriteAnswerList;

    private ChildEventListener mEventListener=new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map=(HashMap)dataSnapshot.getValue();
            String answerUid=dataSnapshot.getKey();

            for(Answer answer:mQuestion.getAnswers()){
                //同じAnswerUidのものが存在しているときは何もしない
                if(answerUid.equals(answer.getAnswerUid())){
                    return;
                }
            }

            String body=(String)map.get("body");
            String name=(String)map.get("name");
            String uid=(String)map.get("uid");

            Answer answer=new Answer(body,name,uid,answerUid);
            mQuestion.getAnswers().add(answer);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
        }
    };

    private ChildEventListener mFavoriteEventListener=new ChildEventListener() {
        //onCreateする際に呼ばれます。
        //ArrayListの中にFavoriteAnswerを挿入します。
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap favoriteAnswerMap=(HashMap)dataSnapshot.getValue();
            //HashMap favoriteAnswerMap=(HashMap)map.get("favorite");
            if(favoriteAnswerMap!=null){
                for(Object key:favoriteAnswerMap.keySet()){
                    HashMap temp=(HashMap)favoriteAnswerMap.get((String)key);
                    String favoriteAnswerName=(String)temp.get("favoriteAnswer");
                    favoriteAnswerList.add((String)favoriteAnswerName);
                }
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        //渡ってきたQuestionのオブジェクトを保持する
        Bundle extras=getIntent().getExtras();
        mQuestion=(Question)extras.get("question");

        setTitle(mQuestion.getTitle());

        //ListViewの準備
        mListView=(ListView)findViewById(R.id.listView);
        mAdapter=new QuestionDetailListAdapter(this,mQuestion);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();


        //favoriteListの作成
        favoriteAnswerList=new ArrayList();
        //FirebaseからReferenceを取得
        DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference();
        //ログイン済みのユーザーを取得
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference favoriteRef=databaseReference.child(Const.UsersPATH).child(user.getUid()).child(Const.FavoritePATH);

        //誤作動をしないようmFavoriteListenerを忘れずにはずす。
        //favoriteRef.removeEventListener(mFavoriteEventListener);
        //favoriteRef.addChildEventListener(mFavoriteEventListener);
        //誤作動をしないようmFavoriteListenerを忘れずにはずす。
        if(favoriteAnswerList!=null) {
            favoriteRef.removeEventListener(mFavoriteEventListener);
        }else{
            favoriteRef.addChildEventListener(mFavoriteEventListener);
        }

        //favoriteButtonの準備
        favoriteButton=(Button)findViewById(R.id.favoriteButton);
        favoriteButton.setOnClickListener(favoriteButtonListener);

        FloatingActionButton fab=(FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //ログイン済みのユーザーを収録する
                FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();

                if(user==null){
                    //ログインしていなければログイン画面に推移させる
                    Intent intent=new Intent(getApplicationContext(),LoginActivity.class);
                    startActivity(intent);
                }else{
                    //Questionを渡して回答作成画面を起動する。
                    Intent intent=new Intent(getApplicationContext(),AnswerSendActivity.class);
                    intent.putExtra("question",mQuestion);
                    startActivity(intent);
                }
            }
        });

        //よくわからない集団。
        //DatabaseReference dataBaseReference= FirebaseDatabase.getInstance().getReference();
        //mAnswerRef=dataBaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child(Const.AnswersPATH);
        //mAnswerRef.addChildEventListener(mEventListener);
        //DatabaseReference testRef=dataBaseReference.child(Const.UsersPATH);
        //testRef.addChildEventListener(mFavoriteEventListener);
    }

    View.OnClickListener favoriteButtonListener= new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //FirebaseからReferenceを取得
            DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference();
            //ログイン済みのユーザーを取得
            FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();

            //現在の質問のuidを取得しHashMapに放り込む
            Map<String,String> favoriteAnswer=new HashMap<String,String>();
            favoriteAnswer.put("favoriteAnswer",mQuestion.getQuestionUid());

            //ログイン済みユーザーのfavoriteスペースを指定
            DatabaseReference favoriteRef=databaseReference.child(Const.UsersPATH).child(user.getUid()).child(Const.FavoritePATH);
            DatabaseReference testRef=databaseReference.child(Const.UsersPATH);

            //favoriteAnswerList内に該当するQuestionUidがあるかどうかを確認
            boolean test=true;
            for(int i=0;i<favoriteAnswerList.size();i++){
                if(favoriteAnswerList.get(i).equals(mQuestion.getQuestionUid())){
                    //ある場合はfavoriteAnswerListから除外する
                    favoriteAnswerList.remove(i);
                    test=false;
                }
            }
            //testがtrueの場合はお気に入りに追加、falseの場合はお気に入りから除外する。
            //(Firebaseのデータから）
            if(test) {
                favoriteAnswerList.add(mQuestion.getQuestionUid());
                favoriteRef.push().setValue(favoriteAnswer);
            }else{
                //favorite(firebase)から要素を除外する
                
            }
        }
    };
}

