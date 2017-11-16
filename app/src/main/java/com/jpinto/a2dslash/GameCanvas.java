package com.jpinto.a2dslash;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.jpinto.a2dslash.characters.Knight;

import java.util.ArrayList;
import stanford.androidlib.SimpleBitmap;
import stanford.androidlib.graphics.GCanvas;
import stanford.androidlib.graphics.GSprite;

/* Created by JPinto on 11/7/2017.*/

public class GameCanvas extends GCanvas{

    // frames per second of animation
    private static final int FRAMES_PER_SECOND = 30;

    // downward acceleration due to gravity
    private static final int GRAVITY_ACCELERATION = 1;

    // private fields
    private Knight knight;
    private Knight enemyArcher;
    private GSprite groundSurface;
    private ArrayList<Knight> enemiesKnightsList = new ArrayList<>();
    private ArrayList<GSprite> arrowList = new ArrayList<>();

    private Bitmap knightIddle;
    private ArrayList<Bitmap> knightWalking;
    private ArrayList<Bitmap> inverseKnightWalking;
    private ArrayList<Bitmap> knightJumping;
    private ArrayList<Bitmap> inverseKnightJumping;
    private ArrayList<Bitmap> knightDying;
    private ArrayList<Bitmap> inverseKnightDying;
    private ArrayList<Bitmap> knightAttacking;
    private ArrayList<Bitmap> inverseKnightAttacking;

    private Bitmap inverseArrow;
    private Bitmap archerArrow;
    private ArrayList<Bitmap> archerWalking;
    private ArrayList<Bitmap> inverseArcherWalking;
    private ArrayList<Bitmap> archerDying;
    private ArrayList<Bitmap> inverseArcherDying;
    private ArrayList<Bitmap> archerAttacking;
    private ArrayList<Bitmap> inverseArcherAttacking;

    private boolean isPlayerAttacking = false;
    private boolean parry = false;
    private boolean isJumping = false;
    private boolean playerDead = false;

    private int frames = 0;

    ConstraintLayout cl_menu;

    public GameCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init() {

        /*GSprite.setDebug(true);*/

        GSprite background = new GSprite(SimpleBitmap.with(this).scale(R.drawable.background,getWidth(),getHeight()));
        add(background);

        groundSurface = new GSprite(SimpleBitmap.with(this)
                .scale(R.drawable.ground,getWidth(),getHeight()/7));
        groundSurface.setBottomY(getHeight());

        add(groundSurface);

        enemyArcher = new Knight();

        loadAssets();
    }
    /*
     * Called by the GCanvas internal animation loop each time the animation ticks,
     * 30 times per second.
     * Updates the sprites and checks for collisions.
     */
    @Override
    public void onAnimateTick() {
        super.onAnimateTick();

        frames++;
        //collision
        if((frames  == 20 || frames % 100 == 0) && enemiesKnightsList.size() < 5 ){
            Knight enemy = new Knight();
            if (frames%200 == 0 ){
                enemy.walkToLeft(inverseKnightWalking);
                add(enemy,getWidth(),(getHeight()*12)/21);
            } else {
                enemy.walkToRight(knightWalking);
                add(enemy,0,(getHeight()*12)/21);
            }

            enemy.setCollisionMargin(getHeight()/9,getHeight()/18);
            enemiesKnightsList.add(enemy);

            enemy.putExtra("isAttacking",false);
            enemy.putExtra("isAttackInCooldown",false);
        }


        if (frames % 400 == 0 && !contains(enemyArcher)) {
            enemyArcher.walkToRight(archerWalking);
            enemyArcher.setCollisionMargin(getHeight() / 9, getHeight() / 18);
            add(enemyArcher, 0, (getHeight() * 12) / 21);
        }

        if((frames == 450 || (frames % 150 == 0 && frames > 450)) && contains(enemyArcher)) {
            enemyArcher.attackToRight(archerAttacking);
            GSprite Arrow = new GSprite();
            Arrow.setVelocityX(10);
            Arrow.setBitmap(archerArrow);
            Arrow.setCollisionMargin(getHeight() / 10, getHeight() / 7);
            add(Arrow,enemyArcher.getX()+enemyArcher.getWidth()/10,enemyArcher.getY());
            arrowList.add(Arrow);
        }

        if (enemiesKnightsList.size()> 0 && !playerDead) enemyKnightCollision();

        if (arrowList.size()> 0 && (!playerDead || enemiesKnightsList.size()> 0))enemyArrowCollision();

        if (frames> 20 && contains(enemyArcher) && !playerDead) enemyArcherCollision();

        jumpCollision();

        /*if (enemiesKnightsList.size()> 0) enemyOutsidePlayingArea();*/

        /*if (arrowList.size()> 0) arrowOutsidePlayingArea();*/
    }

    private void enemyArcherCollision() {
        if (knight.collidesWith(enemyArcher) && isPlayerAttacking){
            enemyArcher.dieToLeft(archerDying);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    enemyArcher.remove();

                }
            }, 1500);
        }
    }


    private void enemyArrowCollision() {

        for (final GSprite arrow : arrowList){

            if (enemiesKnightsList.size() > 0){
                for (final Knight enemy : enemiesKnightsList){
                    if (arrow.collidesWith(enemy)){

                        enemiesKnightsList.remove(enemy);
                        arrowList.remove(arrow);
                        arrow.remove();

                        if (enemy.getVelocityX()> 0){
                            enemy.dieToLeft(knightDying);
                        }else {
                            enemy.dieToLeft(inverseKnightDying);
                        }

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                enemy.remove();
                            }
                        }, 900);
                        break;
                    }
                }
            }
            if (knight.collidesWith(arrow) && isPlayerAttacking){
                arrow.remove();
                arrowList.remove(arrow);
                break;
            }else if (knight.collidesWith(arrow) && isJumping){
                break;
            }else if (knight.collidesWith(arrow) && !isPlayerAttacking && ! isJumping) {
                arrow.remove();
                arrowList.remove(arrow);
                playerDies();
                break;
            }
        }
    }

    private void enemyKnightCollision() {
        for (final Knight enemy : enemiesKnightsList){

            Boolean isEnemyAttacking = enemy.getExtra("isAttacking");
            Boolean isEnemyAttackInCooldown = enemy.getExtra("isAttackInCooldown");

            //Player attack collision detector
            if (knight.collidesWith(enemy) && isPlayerAttacking && !isEnemyAttacking) {
                Log.wtf("Enemy Dead" , "It's a HIT");

                enemy.dieToLeft(inverseKnightDying);
                enemiesKnightsList.remove(enemy);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        enemy.remove();

                    }
                }, 900);
                break;
            }

            //Enemy attack collision detector
            else if (knight.collidesWith(enemy) && !isPlayerAttacking && isEnemyAttacking && !parry) {
                playerDies();
            }

            else if (knight.collidesWith(enemy) && isPlayerAttacking && isEnemyAttacking){
                parry = true;
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        parry = false;
                    }
                }, 1000);
            }

            //Enemy will start attacking
            int distanceBetweenPlayerAndEnemy = (int) (knight.getX() - enemy.getX());

            boolean playerisInFrontEnemy =  distanceBetweenPlayerAndEnemy < 100
                    && 0 < distanceBetweenPlayerAndEnemy;
            boolean  playerisBehindEnemy =  distanceBetweenPlayerAndEnemy > -100
                    && 0 >= distanceBetweenPlayerAndEnemy;

            if(  playerisBehindEnemy && !isEnemyAttackInCooldown ){

                enemy.putExtra("isAttackInCooldown",true);
                enemy.attackToLeft(inverseKnightAttacking);
                enemyAttack(enemy);
            }
            if (playerisInFrontEnemy && !isEnemyAttackInCooldown){

                enemy.putExtra("isAttackInCooldown",true);
                enemy.attackToRight(knightAttacking);
                enemyAttack(enemy);
            }
        }
    }

    public void playerDies(){

        if (knight.getVelocityX()>0){
            knight.dieToRight(knightDying);
        }else{
            knight.dieToLeft(inverseKnightDying);
        }

        playerDead = true;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                knight.remove();
                gameOver();
            }
        }, 1200);
    }

    public void enemyAttack(final Knight enemy){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                enemy.putExtra("isAttacking",true);

            }
        }, 150);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                enemy.setExtra("isAttacking",false);
                enemy.walkToRight(knightWalking);
            }
        }, 850);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                enemy.putExtra("isAttackInCooldown",false);
            }
        }, 5000);
    }

    private void jumpCollision() {
        if(knight.collidesWith(groundSurface)){
            knight.setY((getHeight()*12)/21);
            knight.setVelocityY(0);
            knight.setAccelerationY(0);
            knight.setBitmaps(knight.getVelocityX() > 0 ? knightWalking : inverseKnightWalking);
        }
    }

    private void arrowOutsidePlayingArea() {
        for (GSprite arrow :arrowList){
            if (arrow.getX()> getWidth()){
                remove(arrow);
            }
            arrowList.remove(arrow);
        }
    }

    private void enemyOutsidePlayingArea() {
        for (GSprite enemy :enemiesKnightsList){
            if (enemy.getX()> getWidth()){
                remove(enemy);
            }
            arrowList.remove(enemy);
        }
    }
    /*
     * Called when the user presses or releases their finger from the screen.
     * Causes the rocket to start or stop thrusting.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if(!playerDead && isAnimated()) {

            float x = event.getX();
            float y = event.getY();

            //verify if its jumping
            isJumping = Math.round(knight.getY()) != getHeight() * 12 / 21;

            boolean isRight = knight.getX() + knight.getWidth()/2 < x;
            boolean isLeft = knight.getX() + knight.getWidth()/2 > x;
            boolean isLower = y <= knight.getHeight()*1.5;
            boolean isHigher = y > knight.getHeight()*1.5;
            boolean clickDown = event.getAction() == MotionEvent.ACTION_DOWN;
            boolean attack = false;
            boolean enemyAlive = enemiesKnightsList.size()>0 || arrowList.size()>0 || contains(enemyArcher);
            boolean close =  (x - knight.getCenterX() < 200) && (x - knight.getCenterX() > -200);

            if(enemyAlive && !isPlayerAttacking && close) attack = clickedEnemy(x,y);

            if (isLeft && isHigher && !isJumping && clickDown && !isPlayerAttacking && !attack) {
                // code to run when finger is pressed; begin walking
                knight.walkToLeft(inverseKnightWalking);

            } else if (isRight && isHigher && !isJumping && clickDown && !isPlayerAttacking && !attack) {
                // lifted finger up; stop thrusting
                knight.walkToRight(knightWalking);

            } else if (isLeft && isLower && !isJumping && clickDown && !isPlayerAttacking && !attack) {
                //jumping
                isJumping = true;
                knight.jumpToLeft(inverseKnightJumping);

            } else if (isRight && isLower && !isJumping && clickDown && !isPlayerAttacking && !attack) {
                //jumping
                isJumping = true;
                knight.jumpToRight(knightJumping);

            } else if (isLeft && attack && !isPlayerAttacking && !isJumping) {

                knight.attackToLeft(inverseKnightAttacking);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isPlayerAttacking = true;
                    }
                }, 150);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isPlayerAttacking = false;
                    }
                }, 850);

            } else if (isRight && attack && !isPlayerAttacking && !isJumping) {

                knight.attackToRight(knightAttacking);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isPlayerAttacking = true;
                    }
                }, 150);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isPlayerAttacking = false;
                    }
                }, 850);
            }
        }
        return super.onTouch(v, event);
    }

    public boolean clickedEnemy(float x , float y){
        for (GSprite enemy :enemiesKnightsList){

            boolean enemyClicked = enemy.getCollisionMarginBottom()+ enemy.getCenterY() > y
                    && -enemy.getCollisionMarginTop() + enemy.getCenterY()  < y
                    && enemy.getCollisionMarginRight() + enemy.getCenterX() > x
                    && -enemy.getCollisionMarginLeft() + enemy.getCenterX() < x;

            if (enemyClicked) return true;
        }

        if (enemyArcher.getCollisionMarginBottom()+ enemyArcher.getCenterY() > y
                && -enemyArcher.getCollisionMarginTop() +   enemyArcher.getCenterY() < y
                &&  enemyArcher.getCollisionMarginRight() + enemyArcher.getCenterX() > x
                && -enemyArcher.getCollisionMarginLeft() +  enemyArcher.getCenterX() < x) return true;

        for (GSprite arrow :arrowList){

            boolean enemyClicked = arrow.getCollisionMarginBottom()+ arrow.getCenterY() > y
                    && -arrow.getCollisionMarginTop() + arrow.getCenterY()  < y
                    && arrow.getCollisionMarginRight() + arrow.getCenterX() > x
                    && -arrow.getCollisionMarginLeft() + arrow.getCenterX() < x;

            if (enemyClicked) return true;
        }
        return false;
    }

    public void startGame() {

        animate(FRAMES_PER_SECOND);
        knight = new Knight();
        knight.iddle(knightIddle);
        knight.setCollisionMargin( getHeight()/9,getHeight()/18);
        add(knight,getWidth()/2,(getHeight()*12)/21);
        playerDead = false;
    }

    public void resumeGame(){
        animate(FRAMES_PER_SECOND);
    }

    public void pauseGame() {
        animationStop();
    }

    public void stopGame() {
        animationStop();
        frames = 0;

        remove(knight);
        remove(enemyArcher);

        if (enemiesKnightsList.size()>0){
            for(Knight enemy :enemiesKnightsList){
                remove(enemy);
            }
        }
        if (arrowList.size()>0){
            for (GSprite arrow : arrowList){
                remove(arrow);
            }
        }

        enemiesKnightsList.clear();
        arrowList.clear();

    }

    public void gameOver () {
        stopGame();
        cl_menu.setVisibility(VISIBLE);
        MainActivity.setIsNewGame(true);
    }

    public void setCl_menu(ConstraintLayout cl_menu) {
        this.cl_menu = cl_menu;
    }

    private void loadAssets(){
        float knightHeight = getHeight()/3;

        knightWalking = new ArrayList<>();
        knightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.walk_0, knightHeight));
        knightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.walk_1, knightHeight));
        knightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.walk_2, knightHeight));
        knightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.walk_3, knightHeight));
        knightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.walk_4, knightHeight));
        knightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.walk_5, knightHeight));
        knightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.walk_6, knightHeight));
        knightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.walk_7, knightHeight));
        knightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.walk_8, knightHeight));
        knightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.walk_9, knightHeight));

        inverseKnightWalking = new ArrayList<>();
        inverseKnightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_walk_0, knightHeight));
        inverseKnightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_walk_1, knightHeight));
        inverseKnightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_walk_2, knightHeight));
        inverseKnightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_walk_3, knightHeight));
        inverseKnightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_walk_4, knightHeight));
        inverseKnightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_walk_5, knightHeight));
        inverseKnightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_walk_6, knightHeight));
        inverseKnightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_walk_7, knightHeight));
        inverseKnightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_walk_8, knightHeight));
        inverseKnightWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_walk_9, knightHeight));

        knightJumping = new ArrayList<>();
        knightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.jump_0, knightHeight));
        knightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.jump_1, knightHeight));
        knightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.jump_2, knightHeight));
        knightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.jump_3, knightHeight));
        knightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.jump_4, knightHeight));
        knightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.jump_5, knightHeight));
        knightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.jump_6, knightHeight));
        knightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.jump_7, knightHeight));
        knightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.jump_8, knightHeight));
        knightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.jump_9, knightHeight));

        inverseKnightJumping = new ArrayList<>();
        inverseKnightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_jump_0, knightHeight));
        inverseKnightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_jump_1, knightHeight));
        inverseKnightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_jump_2, knightHeight));
        inverseKnightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_jump_3, knightHeight));
        inverseKnightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_jump_4, knightHeight));
        inverseKnightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_jump_5, knightHeight));
        inverseKnightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_jump_6, knightHeight));
        inverseKnightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_jump_7, knightHeight));
        inverseKnightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_jump_8, knightHeight));
        inverseKnightJumping.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_jump_9, knightHeight));

        knightDying = new ArrayList<>();
        knightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.dead_0, knightHeight));
        knightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.dead_1, knightHeight));
        knightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.dead_2, knightHeight));
        knightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.dead_3, knightHeight));
        knightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.dead_4, knightHeight));
        knightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.dead_5, knightHeight));
        knightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.dead_6, knightHeight));
        knightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.dead_7, knightHeight));
        knightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.dead_8, knightHeight));
        knightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.dead_9, knightHeight));

        inverseKnightDying = new ArrayList<>();
        inverseKnightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_dead_0, knightHeight));
        inverseKnightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_dead_1, knightHeight));
        inverseKnightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_dead_2, knightHeight));
        inverseKnightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_dead_3, knightHeight));
        inverseKnightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_dead_4, knightHeight));
        inverseKnightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_dead_5, knightHeight));
        inverseKnightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_dead_6, knightHeight));
        inverseKnightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_dead_7, knightHeight));
        inverseKnightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_dead_8, knightHeight));
        inverseKnightDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_dead_9, knightHeight));


        knightAttacking = new ArrayList<>();
        knightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.attack_0, knightHeight));
        knightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.attack_1, knightHeight));
        knightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.attack_2, knightHeight));
        knightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.attack_3, knightHeight));
        knightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.attack_4, knightHeight));
        knightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.attack_5, knightHeight));
        knightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.attack_6, knightHeight));
        knightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.attack_7, knightHeight));
        knightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.attack_8, knightHeight));
        knightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.attack_9, knightHeight));


        inverseKnightAttacking = new ArrayList<>();
        inverseKnightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_attack_0, knightHeight));
        inverseKnightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_attack_1, knightHeight));
        inverseKnightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_attack_2, knightHeight));
        inverseKnightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_attack_3, knightHeight));
        inverseKnightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_attack_4, knightHeight));
        inverseKnightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_attack_5, knightHeight));
        inverseKnightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_attack_6, knightHeight));
        inverseKnightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_attack_7, knightHeight));
        inverseKnightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_attack_8, knightHeight));
        inverseKnightAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.iv_attack_9, knightHeight));

        archerAttacking = new ArrayList<>();
        archerAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_shot_0, knightHeight));
        archerAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_shot_1, knightHeight));
        archerAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_shot_2, knightHeight));
        archerAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_shot_3, knightHeight));
        archerAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_shot_4, knightHeight));
        archerAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_shot_5, knightHeight));
        archerAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_shot_6, knightHeight));
        archerAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_shot_7, knightHeight));
        archerAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_shot_8, knightHeight));
        archerAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_shot_9, knightHeight));

        inverseArcherAttacking = new ArrayList<>();
        inverseArcherAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_shot_0, knightHeight));
        inverseArcherAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_shot_1, knightHeight));
        inverseArcherAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_shot_2, knightHeight));
        inverseArcherAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_shot_3, knightHeight));
        inverseArcherAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_shot_4, knightHeight));
        inverseArcherAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_shot_5, knightHeight));
        inverseArcherAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_shot_6, knightHeight));
        inverseArcherAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_shot_7, knightHeight));
        inverseArcherAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_shot_8, knightHeight));
        inverseArcherAttacking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_shot_9, knightHeight));


        archerWalking = new ArrayList<>();
        archerWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_walk_0, knightHeight));
        archerWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_walk_1, knightHeight));
        archerWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_walk_2, knightHeight));
        archerWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_walk_3, knightHeight));
        archerWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_walk_4, knightHeight));
        archerWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_walk_5, knightHeight));
        archerWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_walk_6, knightHeight));
        archerWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_walk_7, knightHeight));
        archerWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_walk_8, knightHeight));
        archerWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_walk_9, knightHeight));

        inverseArcherWalking = new ArrayList<>();
        inverseArcherWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_walk_0, knightHeight));
        inverseArcherWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_walk_1, knightHeight));
        inverseArcherWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_walk_2, knightHeight));
        inverseArcherWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_walk_3, knightHeight));
        inverseArcherWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_walk_4, knightHeight));
        inverseArcherWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_walk_5, knightHeight));
        inverseArcherWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_walk_6, knightHeight));
        inverseArcherWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_walk_7, knightHeight));
        inverseArcherWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_walk_8, knightHeight));
        inverseArcherWalking.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_walk_9, knightHeight));



        archerDying = new ArrayList<>();
        archerDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_die_0, knightHeight));
        archerDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_die_1, knightHeight));
        archerDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_die_2, knightHeight));
        archerDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_die_3, knightHeight));
        archerDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_die_4, knightHeight));
        archerDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_die_5, knightHeight));
        archerDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_die_6, knightHeight));
        archerDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_die_7, knightHeight));
        archerDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_die_8, knightHeight));
        archerDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_die_9, knightHeight));

        inverseArcherDying = new ArrayList<>();
        inverseArcherDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_die_0, knightHeight));
        inverseArcherDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_die_1, knightHeight));
        inverseArcherDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_die_2, knightHeight));
        inverseArcherDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_die_3, knightHeight));
        inverseArcherDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_die_4, knightHeight));
        inverseArcherDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_die_5, knightHeight));
        inverseArcherDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_die_6, knightHeight));
        inverseArcherDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_die_7, knightHeight));
        inverseArcherDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_die_8, knightHeight));
        inverseArcherDying.add(SimpleBitmap.with(this)
                .scaleToHeight(R.drawable.archer_iv_die_9, knightHeight));


        knightIddle = SimpleBitmap.with(this).scaleToHeight(R.drawable.stand_0,getHeight()/3);
        archerArrow = SimpleBitmap.with(this).scaleToHeight(R.drawable.archer_arrow,getHeight()/3);
        inverseArrow = SimpleBitmap.with(this).scaleToHeight(R.drawable.archer_iv_arrow,getHeight()/3);
    }
}
