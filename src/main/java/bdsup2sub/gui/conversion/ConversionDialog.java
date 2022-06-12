/*
 * Copyright 2013 Miklos Juhasz (mjuhasz)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bdsup2sub.gui.conversion;

import java.awt.*;

public class ConversionDialog {

    private final ConversionDialogModel model;
    private final ConversionDialogView view;
    private final ConversionDialogController controller;

    public ConversionDialog(Frame owner) {
        model = new ConversionDialogModel();
        view = new ConversionDialogView(model, owner);
        controller = new ConversionDialogController(model, view);
    }

    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }

    public boolean wasCanceled() {
        return model.wasCanceled();
    }

    public void enableOptionMove(boolean enable) {
        view.enableOptionMove(enable);
    }
}
