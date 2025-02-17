/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.car.app.sample.showcase.common;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.misc.MiscDemoScreen;
import androidx.car.app.sample.showcase.common.navigation.NavigationDemosScreen;
import androidx.car.app.sample.showcase.common.templates.MiscTemplateDemosScreen;
import androidx.car.app.sample.showcase.common.textandicons.TextAndIconsDemosScreen;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;

/** The starting screen of the app. */
public final class StartScreen extends Screen {
    @NonNull
    private final ShowcaseSession mShowcaseSession;

    public StartScreen(@NonNull CarContext carContext, @NonNull ShowcaseSession showcaseSession) {
        super(carContext);
        mShowcaseSession = showcaseSession;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.selectable_lists_demo_title))
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(
                                                        new SelectableListsDemoScreen(
                                                                getCarContext())))
                        .build());
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.task_restriction_demo_title))
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(
                                                        new TaskRestrictionDemoScreen(
                                                                1, getCarContext())))
                        .build());
        listBuilder.addItem(
                new Row.Builder()
                        .setImage(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                R.drawable.ic_map_white_48dp))
                                        .build(),
                                Row.IMAGE_TYPE_ICON)
                        .setTitle(getCarContext().getString(R.string.nav_demos_title))
                        .setOnClickListener(
                                () -> getScreenManager()
                                        .push(new NavigationDemosScreen(getCarContext())))
                        .setBrowsable(true)
                        .build());
        int listLimit = 6;
        // Adjust the item limit according to the car constrains.
        if (getCarContext().getCarAppApiLevel() > CarAppApiLevels.LEVEL_1) {
            listLimit = getCarContext().getCarService(ConstraintManager.class).getContentLimit(
                    ConstraintManager.CONTENT_LIMIT_TYPE_LIST);
        }
        int miscTemplateDemoScreenItemLimit = listLimit;
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.misc_templates_demos_title))
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(new MiscTemplateDemosScreen(
                                                        getCarContext(),
                                                        0,
                                                        miscTemplateDemoScreenItemLimit)))
                        .setBrowsable(true)
                        .build());
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.text_icons_demo_title))
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(new TextAndIconsDemosScreen(getCarContext())))
                        .setBrowsable(true)
                        .build());
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.misc_demo_title))
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(new MiscDemoScreen(getCarContext(),
                                                        mShowcaseSession)))
                        .setBrowsable(true)
                        .build());
        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle(getCarContext().getString(R.string.showcase_demos_title))
                .setHeaderAction(Action.APP_ICON)
                .build();
    }
}