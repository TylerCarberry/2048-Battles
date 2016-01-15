package com.tytanapps.app_2048;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import com.tytanapps.game2048.MainActivity;
import com.tytanapps.game2048.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Test the main fragment containing the emotions and recommendations
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainTest extends ActivityInstrumentationTestCase2<MainActivity> {

    MainActivity mainActivity;


    public MainTest() {
        super(MainActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mainActivity = getActivity();
    }

    /**
     * This test will always pass. If it does not, there is a problem with Android
     */
    @Test
    public void testAlwaysPasses() {
        assertTrue(true);
    }

    /**
     * Verify that everything on the main screen is visible when you start the app
     */
    @Test
    public void testEverythingVisible() {
        onView(withId(R.id.logo_imageview)).check(matches(isDisplayed()));
        onView(withId(R.id.settings_button)).check(matches(isDisplayed()));
        onView(withId(R.id.help_button)).check(matches(isDisplayed()));
        onView(withId(R.id.single_player_imagebutton)).check(matches(isDisplayed()));
        onView(withId(R.id.multiplayer_imagebutton)).check(matches(isDisplayed()));
        onView(withId(R.id.share_button)).check(matches(isDisplayed()));
        onView(withId(R.id.inventory_textview)).check(matches(isDisplayed()));
        onView(withId(R.id.undo_icon)).check(matches(isDisplayed()));
        onView(withId(R.id.undo_inventory)).check(matches(isDisplayed()));
        onView(withId(R.id.powerup_icon)).check(matches(isDisplayed()));
        onView(withId(R.id.powerup_inventory)).check(matches(isDisplayed()));
        onView(withId(R.id.achievements_button)).check(matches(isDisplayed()));
        onView(withId(R.id.leaderboards_button)).check(matches(isDisplayed()));
        onView(withId(R.id.gifts_button)).check(matches(isDisplayed()));
        onView(withId(R.id.quests_button)).check(matches(isDisplayed()));

    }



}