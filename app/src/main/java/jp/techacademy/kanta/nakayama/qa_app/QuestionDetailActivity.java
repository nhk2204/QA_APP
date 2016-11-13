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

    //favoriteAnswerList(ArrayList)を消去
    //private ArrayList<favoriteQuestion> favoriteAnswerList;

    //String NowRef : お気に入りのRefを直接代入する
    private DatabaseReference NowRef;

    //無理やり動かすためにbooleanを追加
    private boolean flagA;

    //質問自体のRefを保存する。
    //コレ自体を比較することでお気に入りか否かの判別を行い、MainActivityにてお気に入りリストを作成する場合も
    //コレを用いて直接呼び出す。
    private DatabaseReference NowQuestionRef;

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

    //-------------------------------------------------------ここまで同じ

    //mFavoriteEventListenerの内容がかなり変更されている。
    private ChildEventListener mFavoriteEventListener=new ChildEventListener() {
        //onCreateする際に呼ばれます。
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            //お気に入りの数を数えていることを確認する。
            System.out.println("There are " + dataSnapshot.getChildrenCount() + " favorites posts");

            //無理やり動かすためのflagAを定義
            flagA=false;

            // お気に入りの数分、下記の処理を実行する
            //dataSnapshot.getChildren()ができるなら、dataSnapshotのidを引っ張ることもできるのではないだろうか・・・？
            for( DataSnapshot snapshot: dataSnapshot.getChildren() )
            {
                favoriteButton=(Button)findViewById(R.id.favoriteButton);
                System.out.println("There are " + snapshot.getRef().toString() + " : getRef()");
                // 選択された質問がお気に入りに追加されていた場合
                /*
                どうにもうまいやり方が見つからないので部分一致で乗り切る。
                NowGenreが完全に意味を失う瞬間である（笑）
                こんなゴミは最早いらない（笑）
                if( NowQuestionRef.toString().equals(snapshot.getValue()))
                */

                //お気に入りかどうかを判別するため、非常に使いにくいsnapshotとかいうゴミクズをわざわざStringに修正してやる
                String snapshotValue;
                snapshotValue=snapshot.getValue().toString();
                String mQuestionUid;
                mQuestionUid=mQuestion.getQuestionUid();
                if(snapshotValue.indexOf(mQuestionUid)!=-1)
                {
                    /*
                    System.out.println("There are " + snapshot.getValue().toString() + " : getValue()");
                    System.out.println("There are " + snapshot.getKey() + " : getKey()");
                    System.out.println("There are " + snapshot.toString() + " : toString()");
                    */

                    NowRef=snapshot.getRef();
                    // お気に入りボタンの状態を変更
                    favoriteButton.setText("お気に入り質問");
                    favoriteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.btn_star_big_on, 0, 0, 0);
                    // お気に入りボタンにタグを１（何でも構わない）とし、登録済みと判定できるようにする。
                    favoriteButton.setTag("1");
                    //ループを抜けます
                    //なぜかbreak出来ない。意味がわからない。
                    //break;

                    //デフォルトをお気に入りに追加されていない状態にし、お気に入りへの追加があった場合のみ変更の動作を行わせる。
                    //flagAをtrueにすることで130行目以降の暴発を防ぐ。
                    //そもそもbreakが使えればこんなことはしなくていいのだが、本当に意味がわからない。
                    /*
                    flagA=true;

                    // 選択された質問がお気に入りに追加されていない場合
                } else {
                    if(flagA!=true) {
                        favoriteButton.setText("お気に入りに追加する");
                        favoriteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.btn_star_big_off, 0, 0, 0);
                        favoriteButton.setTag("0");
                    }
                    */
                }
                //お気に入りボタンを押した際の条件分岐はタグで判断する。
                //タグとは？
            }

            //favoriteAnswerList(ArrayList)でお気に入りの質問をリストアップし覚えておくのではなく、
            //質問ごとにFirebaseから直接情報のやり取りを行う。
/*
            HashMap favoriteAnswerMap=(HashMap)dataSnapshot.getValue();
            //HashMap favoriteAnswerMap=(HashMap)map.get("favorite");
            favoriteQuestion favoriteQuestion=new favoriteQuestion();
            if(favoriteAnswerMap!=null){
                for(Object key:favoriteAnswerMap.keySet()){
                    HashMap temp=(HashMap)favoriteAnswerMap.get((String)key);
                    String favoriteAnswerName=(String)temp.get("favoriteAnswer");
                    String favoriteUid=(String)key;
                    favoriteQuestion.setAnswerName(favoriteAnswerName);
                    favoriteQuestion.setAnswerUid(favoriteUid);
                    favoriteAnswerList.add(favoriteQuestion);
                }
            }
*/
            //下記３行をmFavoriteEventListener内に記述してみる。
            //どんな場合もmEventListenerを発動させることで状況をそろえてみる。
            DatabaseReference dataBaseReference= FirebaseDatabase.getInstance().getReference();
            mAnswerRef=dataBaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child(Const.AnswersPATH);
            mAnswerRef.addChildEventListener(mEventListener);
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
        //favoriteAnswerList=new ArrayList();
        //FirebaseからReferenceを取得
        DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference();
        //ログイン済みのユーザーを取得
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference favoriteRef=databaseReference.child(Const.UsersPATH).child(user.getUid()).child(Const.FavoritePATH);

        //誤作動をしないようmFavoriteListenerを忘れずにはずす。
        //favoriteRef.removeEventListener(mFavoriteEventListener);
        //favoriteRef.addChildEventListener(mFavoriteEventListener);
        //誤作動をしないようmFavoriteListenerを忘れずにはずす。
        //if(favoriteAnswerList.size()!=0) {
        //favoriteRef.removeEventListener(mFavoriteEventListener);
        //}else{
        favoriteRef.addChildEventListener(mFavoriteEventListener);
        //}

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
        DatabaseReference dataBaseReference= FirebaseDatabase.getInstance().getReference();
        NowQuestionRef=dataBaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid());
        //↓2行が復活する。意味はanswerを表示すること。
        //mAnswerRef=dataBaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child(Const.AnswersPATH);
        //mAnswerRef.addChildEventListener(mEventListener);
        //DatabaseReference testRef=dataBaseReference.child(Const.UsersPATH);
        //testRef.addChildEventListener(mFavoriteEventListener);

        //ボタンのデフォルト設定をお気に入りに追加されていない状態にする。
        favoriteButton.setText("お気に入りに追加する");
        favoriteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.btn_star_big_off, 0, 0, 0);
        favoriteButton.setTag("0");
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
            favoriteAnswer.put("favoriteAnswer",NowQuestionRef.toString());

            //ログイン済みユーザーのfavoriteスペースを指定
            DatabaseReference favoriteRef=databaseReference.child(Const.UsersPATH).child(user.getUid()).child(Const.FavoritePATH);
            //DatabaseReference testRef=databaseReference.child(Const.UsersPATH);

            /*
            boolean test=true;
            String deleteAnswerUid="";
            for(int i=0;i<favoriteAnswerList.size();i++){
                if(favoriteAnswerList.get(i).getAnswerName().equals(mQuestion.getQuestionUid())){
                    //ある場合はfavoriteAnswerListから除外する
                    deleteAnswerUid=favoriteAnswerList.get(i).getAnswerUid();
                    favoriteAnswerList.remove(i);
                    test=false;
                }
            }
            */
            //お気に入りに入っているか否かの判別用のArrayListを削除した。
            //ボタンにつけられているタグでお気に入りの質問か否かを判別する。
            if(favoriteButton.getTag().toString().equals("1")){
                //tag:1の場合お気に入りの質問
                //行う動作：お気に入りから除外
                NowRef.removeValue();
                favoriteButton.setText("お気に入りに追加する");
                favoriteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.btn_star_big_off, 0, 0, 0);
                favoriteButton.setTag("0");
            }else{
                //tag:0(1以外）の場合お気に入りの質問でない
                //行う動作：お気に入りに追加
                //favoriteRef.addChildEventListener(mFavoriteEventListener);
                favoriteRef.push().setValue(favoriteAnswer);
            }

            //testがtrueの場合はお気に入りに追加、falseの場合はお気に入りから除外する。
            //(Firebaseのデータから）
            /*
            if(test) {
                favoriteAnswerList.clear();
                //if(favoriteAnswerList.size()!=0) {
                //favoriteRef.removeEventListener(mFavoriteEventListener);
                //}else{
                favoriteRef.addChildEventListener(mFavoriteEventListener);
                //}
                favoriteRef.push().setValue(favoriteAnswer);
                //favoriteRef.addChildEventListener(mFavoriteEventListener);
            }else{
                //favorite(firebase)から要素を除外する
                favoriteRef.removeEventListener(mFavoriteEventListener);
                favoriteRef.child(deleteAnswerUid).removeValue();
            }
            */
        }
    };
}

