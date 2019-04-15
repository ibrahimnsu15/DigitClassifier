package hansha.android.digitclassifier;

import android.graphics.PointF;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.text.DecimalFormat;

import hansha.android.digitclassifier.view.DrawModel;
import hansha.android.digitclassifier.view.DrawView;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener{

    //Formats confidence values into percents.
    public static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("##.#%");

    //Digit classifier model (protobuff file)
    private static final String MODEL_FILE = "file:///android_asset/optimized_mnistNN.pb";
    private static final String INPUT_NODE = "input/x-input";
    private static final String OUTPUT_NODE = "Output";
    private static final float[] DROPOUT = {1};
    private static final long[] INPUT_SIZE = {1,28*28};

    //Load tensorflow library
    static {System.loadLibrary("tensorflow_inference");}
    private TensorFlowInferenceInterface inferenceEngine;

    //Temp variable for confidence values of digit.
    private float[] outputs = new float[10];

    private DrawModel mModel;
    private DrawView mDrawView;
    private View classifyButton;

    private float drawX,drawY;
    private PointF tmpPoint = new PointF();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Init inference engine
        inferenceEngine = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);

        //Init drawing view
        mModel = new DrawModel(28, 28);
        mDrawView = (DrawView) findViewById(R.id.view_draw);
        mDrawView.setModel(mModel);
        mDrawView.setOnTouchListener(this);

        // media player
        final MediaPlayer zero = MediaPlayer.create(this,R.raw.zero);
        final MediaPlayer one = MediaPlayer.create(this,R.raw.one);
        final MediaPlayer two = MediaPlayer.create(this,R.raw.two);
        final MediaPlayer three = MediaPlayer.create(this,R.raw.three);
        final MediaPlayer four = MediaPlayer.create(this,R.raw.four);
        final MediaPlayer five = MediaPlayer.create(this,R.raw.five);
        final MediaPlayer six = MediaPlayer.create(this,R.raw.six);
        final MediaPlayer seven = MediaPlayer.create(this,R.raw.seven);
        final MediaPlayer eight = MediaPlayer.create(this,R.raw.eight);
        final MediaPlayer nine = MediaPlayer.create(this,R.raw.nine);



        classifyButton = findViewById(R.id.buttonClassify);
        classifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inferenceEngine.feed(INPUT_NODE, mDrawView.getPixelData(), INPUT_SIZE);
                inferenceEngine.feed("FC_Layer_1/Dropout/Placeholder", DROPOUT);

                String[] outputNodeArray = {OUTPUT_NODE};
                inferenceEngine.run(outputNodeArray);

                inferenceEngine.fetch(OUTPUT_NODE, outputs);
                int digit = updateReadings(outputs);
                if(digit==0){
                    zero.start();
                }else if(digit==1){
                    one.start();
                }else if(digit==2){
                    two.start();
                }else if(digit==3){
                    three.start();
                }else if(digit==4){
                    four.start();
                }else if(digit==5){
                    five.start();
                }else if(digit==6){
                    six.start();
                }else if(digit==7){
                    seven.start();
                }else if(digit==8){
                    eight.start();
                }else if(digit==9){
                    nine.start();
                }

            }
        });

        View clearButton = findViewById(R.id.buttonClear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mModel.clear();
                mDrawView.reset();
                mDrawView.invalidate();
            }
        });
        int digit = updateReadings(outputs);



    }

    public int updateReadings(float[] outputs) {
        if(outputs.length < 10)
            return -1;
        float max = -1;
        int digit=0;
        for(int i = 0; i < 10; i++) {
            int id = getResources().getIdentifier("textView"+i, "id", getPackageName());
            if(id != 0) {
                TextView textView = (TextView) findViewById(id);
                Spanned text = Html.fromHtml("<b>"+i+": "+"</b>"+PERCENT_FORMAT.format(outputs[i]));
                textView.setText(text);
            }
            if(outputs[i]>max){
                max = outputs[i];
                digit=i;
            }
        }

        return digit;

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_DOWN) {
            processTouchDown(motionEvent);
            return true;

        } else if (action == MotionEvent.ACTION_MOVE) {
            processTouchMove(motionEvent);
            return true;

        } else if (action == MotionEvent.ACTION_UP) {
            processTouchUp();
            return true;
        }
        return false;
    }

    private void processTouchDown(MotionEvent event) {
        drawX = event.getX();
        drawY = event.getY();
        mDrawView.calcPos(drawX, drawY, tmpPoint);
        float previousX = tmpPoint.x;
        float previousY = tmpPoint.y;
        mModel.startLine(previousX, previousY);
    }

    private void processTouchMove(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        mDrawView.calcPos(x, y, tmpPoint);
        float newX = tmpPoint.x;
        float newY = tmpPoint.y;
        mModel.addLineElement(newX, newY);

        drawX = x;
        drawY = y;
        mDrawView.invalidate();
    }

    private void processTouchUp() {
        mModel.endLine();
    }

    @Override
    protected void onResume() {
        mDrawView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mDrawView.onPause();
        super.onPause();
    }
}
