/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.research.ic.ferret;

import com.google.research.ic.ferret.test.Debug;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * TODO: Insert description here. (generated by marknewman)
 */
public class BarPanel extends JPanel {
  public static final int MAX_PIXEL_WIDTH = 100;
  public static final int BAR_HEIGHT = 20;
  public static final Color DEFAULT_COLOR = MainFrame.ACCENT_COLOR;
  
  private Color barColor = DEFAULT_COLOR;
  private double maxModelValue = 0.0;
  private double value = 0.0;
  
  public BarPanel(double val, double max, Color color) {
    barColor = color;
    maxModelValue = max;
    value = val;
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    int width = (int) ((value / maxModelValue) * MAX_PIXEL_WIDTH);
    add(Box.createRigidArea(new Dimension(width, BAR_HEIGHT)));
    
    setPreferredSize(new Dimension(width, BAR_HEIGHT));
    setMinimumSize(new Dimension(width, BAR_HEIGHT));
    Debug.log("Creating BarPanel with dim' " + width + "," + BAR_HEIGHT);
    setBackground(barColor);
    setOpaque(true);
  }
  
  public BarPanel(double val, double max) {
    this(val, max, DEFAULT_COLOR);    
  }
}