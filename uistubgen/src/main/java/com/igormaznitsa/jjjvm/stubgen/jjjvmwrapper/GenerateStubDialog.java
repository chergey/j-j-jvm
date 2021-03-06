/* 
 * Copyright 2015 Igor Maznitsa (http://www.igormaznitsa.com).
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
package com.igormaznitsa.jjjvm.stubgen.jjjvmwrapper;

import com.igormaznitsa.jjjvm.stubgen.jjjvmwrapper.model.ClassItem;
import com.igormaznitsa.jjjvm.stubgen.utils.Utils;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class GenerateStubDialog extends javax.swing.JDialog implements Runnable {

  private static final long serialVersionUID = -2157306308008601632L;

  protected final AtomicReference<String> errorMessage = new AtomicReference<String>();
  protected final AtomicReference<ClassItem[]> classItems = new AtomicReference<ClassItem[]>();
  protected final AtomicReference<String> result = new AtomicReference<String>();
  protected final AtomicBoolean atWork = new AtomicBoolean();

  public GenerateStubDialog(final java.awt.Frame parent) {
    super(parent, true);
    initComponents();
    Utils.toScreenCenter(this);
  }

  public synchronized String process(final ClassItem[] classItems) {
    try {
      this.atWork.set(true);
      this.result.set(null);
      this.errorMessage.set(null);
      this.classItems.set(classItems.clone());

      final Thread thr = new Thread(this);
      thr.setDaemon(true);
      thr.start();
      setVisible(true);
      return result.get();
    }
    catch (Throwable ex) {
      this.errorMessage.set(ex.getMessage());
      ex.printStackTrace();
      return null;
    }
  }

  public String getError() {
    return this.errorMessage.get();
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    buttonCancel = new javax.swing.JButton();
    progressBar = new javax.swing.JProgressBar();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("Stub generating process");
    setResizable(false);

    buttonCancel.setText("Cancel");
    buttonCancel.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonCancelActionPerformed(evt);
      }
    });

    progressBar.setIndeterminate(true);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
          .addComponent(buttonCancel, javax.swing.GroupLayout.Alignment.TRAILING))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(buttonCancel)
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_buttonCancelActionPerformed
    {//GEN-HEADEREND:event_buttonCancelActionPerformed
      this.atWork.set(false);
    }//GEN-LAST:event_buttonCancelActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton buttonCancel;
  private javax.swing.JProgressBar progressBar;
  // End of variables declaration//GEN-END:variables

  @Override
  public void run() {
    try {
      Thread.sleep(500);
      String filledTemplate = new String(Utils.loadResource("template/template.txt"), Charset.forName("UTF-8"));

      SimpleDateFormat p_date = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
      String s_date = p_date.format(new Date());

      filledTemplate = filledTemplate.replace((CharSequence) "${dateTime}", (CharSequence) s_date).replace((CharSequence) "${generator}", (CharSequence) (main.APPLICATION + " " + main.VERSION));

      if (!this.atWork.get()) {
        return;
      }

      final StringBuilder staticFieldsBuffer = new StringBuilder(16384);
      final StringBuilder synamicFieldsBuffer = new StringBuilder(16384);
      final StringBuilder staticMethodsBuffer = new StringBuilder(16384);
      final StringBuilder dynamicMethodsBuffer = new StringBuilder(16384);
      final StringBuilder newInstancesBuffer = new StringBuilder(16384);

      boolean classAdded = false;

      final String MACRO_CLASSNAMEHEAD = replaceSpacerAndEOL("//--------------------------------------------------\n\t// ${className}\n\t//--------------------------------------------------\n");
      final String MACRO_NEWINSTANCE = replaceSpacerAndEOL("\tif (classQualifiedName.equals(\"${qualifiedClassName}\")) normalClassName = \"${className}\";\n");

      final String MACRO_METHOD = replaceSpacerAndEOL("\tif (methodId.equals(\"${methodName}\"))\n\t{\n\t\tthrow new UnsupportedOperationException(\"${normalMethodName}\");\n\t}\n");
      final String MACRO_FIELD = replaceSpacerAndEOL("\tif (fieldId.equals(\"${fieldName}\"))\n\t{\n\t\tthrow new UnsupportedOperationException(\"${normalFieldName}\");\n\t}\n");

      for (int li = 0; li < this.classItems.get().length; li++) {
        if (!this.atWork.get()) {
          return;
        }

        final ClassItem klazz = this.classItems.get()[li];
        final String className = klazz.getJavaClass().getClassName();
        final String classHeader = MACRO_CLASSNAMEHEAD.replace("${className}", className);
        final String jvmFormattedClassName = klazz.getJavaClass().getClassName().replace('.', '/');

        final JavaClass jclazz = klazz.getJavaClass();
        if (!jclazz.isAbstract() && !jclazz.isInterface()) {
          if (classAdded) {
            newInstancesBuffer.append(replaceSpacerAndEOL("\telse\n"));
          }

          String str = MACRO_NEWINSTANCE.replace("${className}", className);
          str = str.replace("${qualifiedClassName}", jvmFormattedClassName);

          newInstancesBuffer.append(str);

          classAdded = true;
        }

        final Field[] staticFields = getFields(klazz, true);
        final Field[] dynamicFields = getFields(klazz, false);
        final Method[] staticMethods = getMethods(klazz, true);
        final Method[] dynamicMethods = getMethods(klazz, false);

        // fill static fields
        if (staticFields.length > 0) {
          staticFieldsBuffer.append(classHeader);

          for (int i = 0; i < staticFields.length; i++) {
            if (!this.atWork.get()) {
              return;
            }

            final Field field = staticFields[i];
            final String fieldNrmalizedName = field2str(field);

            final String fieldId = makeFieldName(jvmFormattedClassName, field);
            if (i > 0) {
              staticFieldsBuffer.append(replaceSpacerAndEOL("\telse\n"));
            }

            String str = MACRO_FIELD.replace("${fieldName}", fieldId);
            str = str.replace("${normalFieldName}", fieldNrmalizedName);

            staticFieldsBuffer.append(str);
          }
        }

        // fill dynamic fields
        if (dynamicFields.length > 0) {
          synamicFieldsBuffer.append(classHeader);

          for (int i = 0; i < dynamicFields.length; i++) {
            if (!this.atWork.get()) {
              return;
            }

            final Field field = dynamicFields[i];
            final String fieldNormalName = field2str(field);

            final String fieldId = makeFieldName(jvmFormattedClassName, field);

            if (i > 0) {
              synamicFieldsBuffer.append(replaceSpacerAndEOL("\telse\n"));
            }

            String s_str = MACRO_FIELD.replace("${fieldName}", fieldId);
            s_str = s_str.replace("${normalFieldName}", fieldNormalName);

            synamicFieldsBuffer.append(s_str);
          }
        }

        // fill static methods
        if (staticMethods.length > 0) {
          staticMethodsBuffer.append(classHeader);

          for (int i = 0; i < staticMethods.length; i++) {
            if (!this.atWork.get()) {
              return;
            }

            final Method p_method = staticMethods[i];

            final String methodId = makeMethodName(jvmFormattedClassName, p_method);
            final String methodNormalName = method2str(p_method);

            if (i > 0) {
              staticMethodsBuffer.append(replaceSpacerAndEOL("\telse\n"));
            }

            String str = MACRO_METHOD.replace("${methodName}", methodId);
            str = str.replace("${normalMethodName}", methodNormalName);

            staticMethodsBuffer.append(str);
          }
        }

        // fill dynamic methods
        if (dynamicMethods.length > 0) {
          dynamicMethodsBuffer.append(classHeader);

          for (int i = 0; i < dynamicMethods.length; i++) {
            if (!this.atWork.get()) {
              return;
            }

            final Method method = dynamicMethods[i];
            final String methodId = makeMethodName(jvmFormattedClassName, method);
            final String methodNormalName = method2str(method);

            if (i > 0) {
              dynamicMethodsBuffer.append(replaceSpacerAndEOL("\telse\n"));
            }

            String str = MACRO_METHOD.replace("${methodName}", methodId);
            str = str.replace("${normalMethodName}", methodNormalName);

            dynamicMethodsBuffer.append(str);
          }
        }

      }

      filledTemplate = filledTemplate.replace("${nonStaticFields}", synamicFieldsBuffer.toString());
      filledTemplate = filledTemplate.replace("${nonStaticMethods}", dynamicMethodsBuffer.toString());
      filledTemplate = filledTemplate.replace("${newInstances}", "");//newInstancesBuffer.toString());
      filledTemplate = filledTemplate.replace("${staticFields}", staticFieldsBuffer.toString());
      filledTemplate = filledTemplate.replace("${staticMethods}", staticMethodsBuffer.toString());
      filledTemplate = filledTemplate.replace("${newMultidimensionObjectArray}", "");
      filledTemplate = filledTemplate.replace("${newObjectArray}", "");

      this.result.set(filledTemplate);
    }
    catch (Throwable ex) {
      ex.printStackTrace();

      this.errorMessage.set(ex.getMessage());
      result.set(null);

      return;
    }
    finally {
      setVisible(false);
    }
  }

  private static String replaceSpacerAndEOL(final String str) {
    return str.replace("\t", "    ").replace("\n", System.getProperty("line.separator","\n"));
  }

  protected String makeFieldName(final String className, final Field field) {
    return className + '.' + field.getName() + '.' + field.getSignature();
  }

  protected String makeMethodName(final String className, final Method method) {
    return className + '.' + method.getName() + '.' + method.getSignature();
  }

  protected Field[] getFields(final ClassItem classItem, final boolean staticFields) {
    final Set<Field> fieldSet = new TreeSet<Field>(new Comparator<Field>() {

      @Override
      public int compare(final Field o1, final Field o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });

    final Field[] fields = classItem.getJavaClass().getFields();
    for (final Field field : fields) {
      if (staticFields) {
        if (field.isStatic()) {
          fieldSet.add(field);
        }
      } else {
        if (!field.isStatic()) {
          fieldSet.add(field);
        }
      }
    }

    return fieldSet.toArray(new Field[fieldSet.size()]);
  }

  protected Method[] getMethods(final ClassItem classItem, final boolean staticMethods) {
    final Set<Method> methodSet = new TreeSet<Method>(new Comparator<Method>() {
      @Override
      public int compare(final Method o1, final Method o2) {
        String s_name = o1.getName();
        String s_name2 = o2.getName();

        if (s_name.equals(s_name2)) {
          return 1;
        }
        return o1.getName().compareTo(o2.getName());
      }
    });

    final Method[] methods = classItem.getJavaClass().getMethods();
    for (final Method method : methods) {
      if (method.isPrivate()) {
        continue;
      }

      if (staticMethods) {
        if (method.isStatic()) {
          methodSet.add(method);
        }
      } else {
        if (!method.isStatic()) {
          methodSet.add(method);
        }
      }
    }

    return methodSet.toArray(new Method[methodSet.size()]);
  }

  protected String field2str(final Field field) {
    String modifier = "";

    if (field.isPrivate()) {
      modifier = "private ";
    } else if (field.isProtected()) {
      modifier = "protected ";
    } else if (field.isPublic()) {
      modifier = "public ";
    }

    if (field.isStatic()) {
      modifier += "static ";
    }

    if (field.isFinal()) {
      modifier += "final ";
    }

    modifier += field.getType().toString();

    modifier += ' ' + field.getName();

    return modifier;
  }

  protected String method2str(final Method method) {
    String modifier = "";

    if (method.isPrivate()) {
      modifier = "private ";
    } else if (method.isProtected()) {
      modifier = "protected ";
    } else if (method.isPublic()) {
      modifier = "public ";
    }

    if (method.isStatic()) {
      modifier += "static ";
    }

    if (method.isFinal()) {
      modifier += "final ";
    }

    modifier += method.getReturnType().toString();

    modifier += ' ' + method.getName();

    final StringBuilder buffer = new StringBuilder();
    org.apache.bcel.generic.Type[] argTypes = method.getArgumentTypes();

    for (int li = 0; li < argTypes.length; li++) {
      if (li > 0) {
        buffer.append(", ");
      }
      buffer.append(argTypes[li].toString());
    }

    modifier += '(' + buffer.toString() + ')';

    return modifier;
  }

}
