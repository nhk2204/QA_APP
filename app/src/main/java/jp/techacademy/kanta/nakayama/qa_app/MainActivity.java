package jp.techacademy.kanta.nakayama.qa_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private int mGenre=0;

    private DatabaseReference mDatabaseReference;
    private DatabaseReference mGenreRef;
    private ListView mListView;
    private ArrayList<Question> mQuestionArrayList;
    private QuestionsListAdapter mAdapter;

    //FavoriteからArrayListを作成
    private ArrayList<String> mFavoriteQuestionArrayList=new ArrayList<String>();
    //とにかく動かすためにbooleanで対処
    //private boolean flagA;
    //とにかく動かすためにintを作成
    private int NowGenre;

    //ChildEventListenerの呼び出されるタイミングがよくわからないのでかなり面倒くさい処理を行っている。
    //動かした感じの想像だが、おそらくだが「DatabaseReferenceが更新された後、メソッドの終わりに1度だけ呼ばれる」と思われる。
    private ChildEventListener mFavoriteListenerMainEx=new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            //ほぼmEventListenerのコピペ。
            //異なる箇所はmFavoriteQuestionArrayListにより項目を振り分ける点。
            //QuestionクラスとAnswerクラスを作成しArrayListに追加する。
            if(mFavoriteQuestionArrayList.size()!=0) {
                for (int i = 0; i < mFavoriteQuestionArrayList.size(); i++) {
                    if (("{favoriteAnswer="+dataSnapshot.getRef().toString()+"}").equals(mFavoriteQuestionArrayList.get(i))) {
                        HashMap map = (HashMap) dataSnapshot.getValue();
                        String title = (String) map.get("title");
                        String body = (String) map.get("body");
                        String name = (String) map.get("name");
                        String uid = (String) map.get("uid");
                        String imageString = (String) map.get("image");
                        Bitmap image = null;
                        byte[] bytes;
                        if (imageString != null) {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            bytes = Base64.decode(imageString, Base64.DEFAULT);
                        } else {
                            bytes = new byte[0];
                        }

                        /*
                        何故使用できなくなったのかは皆目見当がつかないが、HashMap answerMap=(HashMap)Map.get("answers")が
                        必ずnullになるという気違い仕様。今までできていたことができなくなるのは何故なのか？
                        本当に理解できない。
                        HashMap answerMap = (HashMap) map.get("answers");
                        ↑この文章はとても理にかなっているように感じるが、Androidはどういう風に解釈しているのか、理解を示さないようである。実に不思議である。

                        追記：
                        なぜか文章をまったく変更していないにもかかわらず正しく動くようになった。
                        まさに意味不明、奇奇怪怪という言葉がよく似合う。つまり今までできるのにやっていなかったわけだ。
                        */
                        ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
                        HashMap answerMap = (HashMap) map.get("answers");
                        if (answerMap != null) {
                            for (Object key : answerMap.keySet()) {
                                HashMap temp = (HashMap) answerMap.get((String) key);
                                String answerBody = (String) temp.get("body");
                                String answerName = (String) temp.get("name");
                                String answerUid = (String) temp.get("uid");
                                Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                                answerArrayList.add(answer);
                            }
                        }

                        //Questionの引数のGenreをNowGenreに変更
                        //コレするくらいならGenreをそもそも更新したほうがいい気もする・・・
                        //Genreを999にする。
                        //質問を書き込む際はQuestionUidから探させることにする。

                        //無理やりGenreをそろえることにする。
                        String snapshotRef;
                        String snapshotKey;
                        int EndPoint;
                        snapshotRef=dataSnapshot.getRef().toString();
                        snapshotKey=dataSnapshot.getKey();
                        EndPoint=snapshotRef.indexOf(snapshotKey);

                        int ThisGenre;
                        ThisGenre=Integer.parseInt(snapshotRef.substring(EndPoint-2,EndPoint-1));
                        //ここまでGenreをそろえる動作。
                        //Genreの数が2桁になると割と面倒くさい変換が必要になる気がするがとりあえず"無視"を決め込む！！

                        Question question = new Question(title, body, name, uid, dataSnapshot.getKey(), ThisGenre, bytes, answerArrayList);
                        mQuestionArrayList.add(question);
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }
            if(NowGenre<4) {
                NowGenre += 1;
                mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(NowGenre));
                mGenreRef.addChildEventListener(mFavoriteListenerMainEx);

                //お気に入り画面だけ違う動作をするので、お気に入り画面だけ違う動作をさせている部分を削除してみようと思い、
                //以下の分岐を作成。
                //お気に入りを作成後mEventListenerが作動する。
                //が、コレによって何が変わるかといわれるとまったく分からない。

                //追記：当てが外れたので削除。
                //onChildChangedにmAdapterが振り分けられていないのが原因ではないだろうか？
                //}else{
                //mGenreRef.addChildEventListener(mEventListener);
            }
        }
        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            //とりあえずそのままコピペ（mEventListenerから）。
            //コレで大丈夫なのだろうか・・・？
            //↑だめfavoriteをチェックし必要に応じて削除する命令を作成する必要がある。

            /*
            //そんなわけで、リストの作成から最初からやり直す方式に変更。
            //めんどくさいことを多々行うが、おそらくコレが確実であると思う。
            ↑
            想像以上に頭の悪い解釈をされたので削除。
            なるほどと感心するほどに一番都合の悪い解釈である。
            （ListViewのクリアがなされないためListが長くなる。
            また、お気に入りのチェック項目はfirebaseの質問欄の更新を行っていないため
            Firebaseの更新とみなされないらしくお気に入りが外れたことに対するアクションがなされない。
            実に最悪である。考えうる限り最も都合の悪い解釈をなされている。Firebase様は賢くあらせられる。）

            //選択したジャンルのデータをクローズして再度ジャンルを読み込む準備をする。
            if(mGenreRef!=null){
                //データの初期化
                mGenreRef.removeEventListener(mEventListener);
                mGenreRef.removeEventListener(mFavoriteListenerMain);
                //mFavoriteQuestionArrayList.clear();
            }
            //お気に入りが保存してある場所を開く
            //FirebaseからReferenceを取得
            DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference();
            //ログイン済みのユーザーを取得
            FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
            mGenreRef=databaseReference.child(Const.UsersPATH).child(user.getUid());//.child(Const.FavoritePATH);
            mGenreRef.addChildEventListener(mFavoriteListenerMain);
            */

            //要素に変化があった際に呼ばれる。
            //今回は質問に対して回答が投稿されたときに呼ばれます。
            //Questionクラスのインスタンスが保持している回答のArrayListを一回クリアし取得した回答を設定する。
            HashMap map=(HashMap)dataSnapshot.getValue();

            //変更があったQuestionを探す
            //AnswerSendActivity画面で回答を投稿した際に動いている。何故？？？
            for(Question question:mQuestionArrayList){

                //よくよく考えたらここでする必要なくないか？
                //boolean favoriteCheck;
                //favoriteCheck=false;

                if(dataSnapshot.getKey().equals(question.getQuestionUid())){
                    //回答(Answer)の変更チェックを行う。
                    question.getAnswers().clear();
                    HashMap answerMap=(HashMap)map.get("answers");
                    if(answerMap!=null){
                        for(Object key:answerMap.keySet()){
                            HashMap temp=(HashMap)answerMap.get((String)key);
                            String answerBody=(String)temp.get("body");
                            String answerName=(String)temp.get("name");
                            String answerUid=(String)temp.get("uid");
                            Answer answer=new Answer(answerBody,answerName,answerUid,(String)key);
                            question.getAnswers().add(answer);
                        }
                    }
                    //favoriteCheck=true;
                    mAdapter.notifyDataSetChanged();
                }

                /*
                チェックをはずしたときに動かすべき（？）
                if(favoriteCheck==false){
                    mAdapter.notifyDataSetChanged();
                }
                */
            }
        }
        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            //削除されたときに更新を行う（？）
            //よく分からんけどメニューからお気に入りを選んだ際に動く
            //mAdapter.notifyDataSetChanged();

            //HashMap map=(HashMap)dataSnapshot.getValue();

        }
        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            //削除されたときに更新を行う（？）
            //よく分からんけどメニューからお気に入りを選んだ際に動く
            //mAdapter.notifyDataSetChanged();
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            //削除されたときに更新を行う（？）
            //よく分からんけどメニューからお気に入りを選んだ際に動く
            //mAdapter.notifyDataSetChanged();
        }
    };

    //mQuestionArrayListを作成する。
    private ChildEventListener mFavoriteListenerMain=new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            mFavoriteQuestionArrayList.clear();
            for( DataSnapshot snapshot: dataSnapshot.getChildren() )
            {
                mFavoriteQuestionArrayList.add(snapshot.getValue().toString());
            }
            if(mGenreRef!=null){
                //データの初期化
                mGenreRef.removeEventListener(mEventListener);
                mGenreRef.removeEventListener(mFavoriteListenerMain);
            }
            NowGenre=1;
            mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(NowGenre));
            mGenreRef.addChildEventListener(mFavoriteListenerMainEx);
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

    private ChildEventListener mEventListener= new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            //要素が追加されたときに呼ばれるメソッド。
            //今回だと質問が追加されたときに呼ばれる。
            //QuestionクラスとAnswerクラスを作成しArrayListに追加する。
            HashMap map=(HashMap)dataSnapshot.getValue();
            String title=(String)map.get("title");
            String body=(String)map.get("body");
            String name=(String)map.get("name");
            String uid=(String)map.get("uid");
            String imageString=(String)map.get("image");
            Bitmap image=null;
            byte[] bytes;
            if(imageString!=null){
                BitmapFactory.Options options=new BitmapFactory.Options();
                bytes= Base64.decode(imageString,Base64.DEFAULT);
            }else{
                bytes=new byte[0];
            }

            ArrayList<Answer> answerArrayList=new ArrayList<Answer>();
            HashMap answerMap=(HashMap)map.get("answers");
            if(answerMap!=null){
                for(Object key:answerMap.keySet()){
                    HashMap temp=(HashMap)answerMap.get((String)key);
                    String answerBody=(String)temp.get("body");
                    String answerName=(String)temp.get("name");
                    String answerUid=(String)temp.get("uid");
                    Answer answer=new Answer(answerBody,answerName,answerUid,(String)key);
                    answerArrayList.add(answer);
                }
            }

            Question question=new Question(title,body,name,uid,dataSnapshot.getKey(),mGenre,bytes,answerArrayList);
            mQuestionArrayList.add(question);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            //要素に変化があった際に呼ばれる。
            //今回は質問に対して回答が投稿されたときに呼ばれます。
            //Questionクラスのインスタンスが保持している回答のArrayListを一回クリアし取得した回答を設定する。
            HashMap map=(HashMap)dataSnapshot.getValue();

            //変更があったQuestionを探す
            for(Question question:mQuestionArrayList){
                if(dataSnapshot.getKey().equals(question.getQuestionUid())){
                    //このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.getAnswers().clear();
                    HashMap answerMap=(HashMap)map.get("answers");
                    if(answerMap!=null){
                        for(Object key:answerMap.keySet()){
                            HashMap temp=(HashMap)answerMap.get((String)key);
                            String answerBody=(String)temp.get("body");
                            String answerName=(String)temp.get("name");
                            String answerUid=(String)temp.get("uid");
                            Answer answer=new Answer(answerBody,answerName,answerUid,(String)key);
                            question.getAnswers().add(answer);
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                }
            }
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
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        //NowGenreの初期化
        NowGenre=999;

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //ジャンルを選択していない場合(mGenre==0)はエラーを表示する
                if(mGenre==0){
                    Snackbar.make(view,"ジャンルを選択してください",Snackbar.LENGTH_LONG).show();
                    return;
                }else if(mGenre==5){
                    Snackbar.make(view,"お気に入り画面で質問の作成はできません",Snackbar.LENGTH_LONG).show();
                    return;
                }
                //ログイン済みのユーザーを収録する
                FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();

                //ログインしていなければログイン画面に推移させる
                if(user==null){
                    Intent intent=new Intent(getApplicationContext(),LoginActivity.class);
                    startActivity(intent);
                }else{
                    //ログインしている場合はジャンルを渡して質問作成画面を起動する
                    Intent intent =new Intent(getApplicationContext(),QuestionSendActivity.class);

                    intent.putExtra("genre",mGenre);
                    startActivity(intent);
                }
            }
        });

        //ナビゲーションドロワーの設定
        DrawerLayout drawer=(DrawerLayout)findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle=new ActionBarDrawerToggle(this,drawer,mToolbar,R.string.app_name,R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView=(NavigationView)findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id=item.getItemId();

                if(id==R.id.nav_hobby){
                    mToolbar.setTitle("趣味");
                    mGenre=1;
                }else if(id==R.id.nav_life){
                    mToolbar.setTitle("生活");
                    mGenre=2;
                }else if(id==R.id.nav_health){
                    mToolbar.setTitle("健康");
                    mGenre=3;
                }else if(id==R.id.nav_computre){
                    mToolbar.setTitle("コンピューター");
                    mGenre=4;
                }else{
                    mToolbar.setTitle("お気に入り");
                    mGenre=5;
                }

                DrawerLayout drawer=(DrawerLayout)findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);

                //質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
                mQuestionArrayList.clear();
                mAdapter.setQuestionArrayList(mQuestionArrayList);
                mListView.setAdapter(mAdapter);

                //選択したジャンルのデータをクローズして再度ジャンルを読み込む準備をする。
                if(mGenreRef!=null){
                    //データの初期化
                    mGenreRef.removeEventListener(mEventListener);
                    mGenreRef.removeEventListener(mFavoriteListenerMain);
                    //mFavoriteQuestionArrayList.clear();
                }

                //お気に入りを呼び出したときとそれ以外のときとで場合わけ。
                if(mGenre==5){
                    //お気に入りが保存してある場所を開く
                    //FirebaseからReferenceを取得
                    DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference();
                    //ログイン済みのユーザーを取得
                    FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
                    mGenreRef=databaseReference.child(Const.UsersPATH).child(user.getUid());//.child(Const.FavoritePATH);
                    mGenreRef.addChildEventListener(mFavoriteListenerMain);
                    //flagA=true;
                    /*
                    for(int i=1;i<5;i++) {
                        if(mGenreRef!=null){
                            //データの初期化
                            mGenreRef.removeEventListener(mEventListener);
                            mGenreRef.removeEventListener(mFavoriteListenerMain);
                        }
                        mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(1));
                        mGenreRef.addChildEventListener(mFavoriteListenerMainEx);
                    }
                    */
                }else {
                    //データベースのルートを示している。
                    //持ってくる場所をセット。mGenreRefに場所を登録
                    mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mGenre));
                    //データを持ってくるよう命ずる。
                    mGenreRef.addChildEventListener(mEventListener);
                }
                return true;
            }
        });

        //Firebase
        //Firebaseを使う準備をする
        mDatabaseReference= FirebaseDatabase.getInstance().getReference();

        //ListViewの準備
        mListView=(ListView)findViewById(R.id.listView);
        mAdapter=new QuestionsListAdapter(this);
        mQuestionArrayList=new ArrayList<Question>();
        mAdapter.notifyDataSetChanged();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Questionのインスタンスを渡して質問詳細画面を起動する
                Intent intent=new Intent(getApplicationContext(),QuestionDetailActivity.class);

                intent.putExtra("question",mQuestionArrayList.get(position));
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent=new Intent(getApplicationContext(),SettingActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
