package com.jakewharton.rxbinding2.widget;

import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import com.jakewharton.rxbinding2.test.R;
import com.jakewharton.rxbinding2.RecordingObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public final class RxAutoCompleteTextViewTest {
  @Rule public final ActivityTestRule<RxAutoCompleteTextViewTestActivity> activityRule =
      new ActivityTestRule<>(RxAutoCompleteTextViewTestActivity.class);

  private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

  private RxAutoCompleteTextViewTestActivity activity;
  private AutoCompleteTextView autoCompleteTextView;

  @Before public void setUp() {
    activity = activityRule.getActivity();
    autoCompleteTextView = activity.autoCompleteTextView;
  }

  @Test public void itemClickEvents() {
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        autoCompleteTextView.setThreshold(1);

        List<String> values = Arrays.asList("Two", "Three", "Twenty");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(autoCompleteTextView.getContext(),
            android.R.layout.simple_list_item_1, values);
        autoCompleteTextView.setAdapter(adapter);
      }
    });

    RecordingObserver<AdapterViewItemClickEvent> o = new RecordingObserver<>();
    RxAutoCompleteTextView.itemClickEvents(autoCompleteTextView)
      .subscribeOn(AndroidSchedulers.mainThread())
      .subscribe(o);
    o.assertNoMoreEvents();

    onView(withId(R.id.auto_complete)).perform(typeText("Tw"));
    onData(startsWith("Twenty"))
        .inRoot(withDecorView(not(is(activity.getWindow().getDecorView()))))
        .perform(click());

    AdapterViewItemClickEvent event = o.takeNext();
    assertNotNull(event.view());
    assertNotNull(event.clickedView());
    assertEquals(1, event.position()); // Second item in two-item filtered list.
    assertEquals(1, event.id()); // Second item in two-item filtered list.

    o.dispose();

    onView(withId(R.id.auto_complete)).perform(clearText(), typeText("Tw"));
    onData(startsWith("Twenty"))
        .inRoot(withDecorView(not(is(activity.getWindow().getDecorView()))))
        .perform(click());

    o.assertNoMoreEvents();
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
  @Test @UiThreadTest public void completionHint() throws Exception {
    RxAutoCompleteTextView.completionHint(autoCompleteTextView).accept("Test hint");
    assertEquals("Test hint", autoCompleteTextView.getCompletionHint());
  }

  @Test @UiThreadTest public void threshold() throws Exception {
    RxAutoCompleteTextView.threshold(autoCompleteTextView).accept(10);
    assertEquals(10, autoCompleteTextView.getThreshold());
  }
}
