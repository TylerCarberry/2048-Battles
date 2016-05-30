![2048 Battles Logo](https://cloud.githubusercontent.com/assets/6628497/4520300/6df7cce6-4ce6-11e4-935a-e687fe85379d.png).
![Main Screen Screenshor](https://cloud.githubusercontent.com/assets/6628497/7486726/716bf914-f377-11e4-9627-434a3c9da39e.png).
![Multiplayer](https://cloud.githubusercontent.com/assets/6628497/7486728/73e674ee-f377-11e4-9ed2-82e95362735f.png)



# 2048 Battles [![Circle CI](https://circleci.com/gh/TylerCarberry/2048-Battles.svg?style=svg)](https://circleci.com/gh/TylerCarberry/2048-Battles)

[![Available on Google Play](https://cloud.githubusercontent.com/assets/6628497/12313130/db2133a6-ba32-11e5-883f-636fac12c0cb.png)](https://play.google.com/store/apps/details?id=com.tytanapps.game2048)  
My version of 2048 based off the orginal 2048 by Gabriele Cirulli.  

### Real Time Multiplayer
Challenge a random opponent to a quick battle.  
Send attacks or use powerups to win.

### Custom Game Modes
Create a 2x3 game or 15x15  
Toggle any of the game modes below individually

### 9 Different Game Modes
Classic: The boring way of playing  
Practice: Unlimited powerups. Warns you before you lose.  
Arcade: Randomly gain powerups and attacks   
XMode: XTile cannot be combined  
Corner Mode: Immovable tiles in the corners  
Rush: Higher value tiles appear  
Ghost: All tiles appear as ?  
Crazy: A 5x5 grid with all other game modes enabled  
Survival: 15 seconds. Combine tiles for more time  
Coming Soon: Zen Mode

![Custom Mode](https://cloud.githubusercontent.com/assets/6628497/7486729/756d17dc-f377-11e4-8abe-4178ccb827a5.png). 
![Power Up](https://cloud.githubusercontent.com/assets/6628497/7486888/b3934998-f37a-11e4-956d-0712a96874e6.png). 
![Confirm Move](https://cloud.githubusercontent.com/assets/6628497/7486886/b041aae6-f37a-11e4-900a-24c3266cd8b1.png)



### Powerups
Undo  
Shuffle  
Remove A Tile  
Remove All Low Tiles: Remove all 2s and 4s  
Warn Before You Lose  

### Additional Features
Achievements  
Leaderboards  
Quests  
Gifts: Send a powerup to your Google+ friend  
Coming Soon: Cloud Save

![Achievements](https://cloud.githubusercontent.com/assets/6628497/7486882/ada5b4f8-f37a-11e4-8ced-187b09d215a2.png). 
![Quests](https://cloud.githubusercontent.com/assets/6628497/7486884/aed4bbbc-f37a-11e4-8704-2e2a1a5726ef.png). 
![Make Custom Game](https://cloud.githubusercontent.com/assets/6628497/7486727/729742da-f377-11e4-8df0-246ce723d227.png)

## Modifying the code
1. Copy [ids.xml](https://gist.github.com/TylerCarberry/e61d218acc41a67b628c) to app2048/src/main/res/values/ids.xml  
2. Copy [app_tracker.xml](https://gist.github.com/TylerCarberry/79bbdafbaedbed79f106) to app2048/src/main/res/xml/app_tracker.xml  
3. (Optional) Create a new game service on the Google Play Developer Console. Create new achievements, leaderboards, and events to match the ids. Copy your own Google Play Games ids to the previous two files. The app with function without it but you will not have access to Google Play Games, including multiplayer.

**Licensing:** This app is protected by the MIT License. You may use and modify the code as described in [LICENSE.txt](LICENSE.txt)
