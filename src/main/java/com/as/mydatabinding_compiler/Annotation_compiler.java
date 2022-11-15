package com.as.mydatabinding_compiler;

import com.as.mydatabinding.BindView;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.JavaFileObject;

/**
 * 注解处理器
 * 1.继承
 * 2.注册注解处理器
 */
@AutoService(Processor.class)
public class Annotation_compiler extends AbstractProcessor {
    //生成文件的对象
    Filer filer=null;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer=processingEnvironment.getFiler();
    }
    /**
     * 声明要支持的注解有哪些
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> set=new HashSet<>();
        set.add(BindView.class.getCanonicalName());
        return set;
    }
    /**
     * 声明支持的JAVA的版本
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return processingEnv.getSourceVersion();
    }

    /**
     * 核心方法 这个方法可以得到注解标记的内容
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //获得当前模块中所有用到BindView注解的节点
        //节点
        //类节点TypeElement
        //方法节点 ExecutableElement
        //成员变量节点VariableElement
        Set<? extends Element> elements=roundEnvironment.getElementsAnnotatedWith(BindView.class);
        //map的结构，key代表每个activity，对应的list代表当前activity所对应的所有被bindview注解修饰过的成员变量
        Map<String, List<VariableElement>> listMap=new HashMap<>();
        for(Element element:elements)
        {
            //转化一下，得到的是成员变量的节点
            VariableElement variableElement=(VariableElement) element;
            //获取上个节点，然后再获取上个节点的类名
            TypeElement typeElement=(TypeElement) variableElement.getEnclosingElement();
            String activityName=typeElement.getSimpleName().toString();
            //先判断map里对应的key有没有存在list，没有新建，然后往list里添加对应的节点
            List<VariableElement> list=listMap.get(activityName);
            if(list==null)
            {
                list=new ArrayList<>();
                listMap.put(activityName,list);
            }
            list.add(variableElement);
        }



        if(listMap.size()>0)
        {
            Writer writer=null;
            //得到map的key迭代器
            Iterator<String> iterator=listMap.keySet().iterator();
            while (iterator.hasNext())
            {
                String activityName=iterator.next();
                List<VariableElement> list=listMap.get(activityName);
                //获取包名
                String packageName=getPackageName(list.get(0));
                //组装类名
                String newName=activityName+"$$ViewBinder";
                try{
                   JavaFileObject javaFileObject= filer.createSourceFile(packageName+"."+newName);
                    writer=javaFileObject.openWriter();
                    StringBuffer buffer=new StringBuffer();
                    buffer.append("package "+packageName+";\n");
                    buffer.append("import android.view.View;\n");
                    buffer.append("public class "+newName+"{\n");
                    buffer.append("public "+newName+"(final "+packageName+"."+activityName+" target){\n");
                    for(VariableElement variableElement:list)
                    {
                        //获取成员变量名字
                        String fieldName=variableElement.getSimpleName().toString();
                        //获得注解的id
                        int resId=variableElement.getAnnotation(BindView.class).value();
                        boolean isClick=variableElement.getAnnotation(BindView.class).isClick();
                        buffer.append("target."+fieldName+"=target.findViewById("+resId+");\n");
                        if(isClick)
                            buffer.append("target."+fieldName+".setOnClickListener(target);\n");

                    }
                    buffer.append("}\n}");
                    writer.write(buffer.toString());
                }catch (Exception e)
                {
                    e.printStackTrace();
                }finally {
                    if(writer!=null)
                    {
                        try {
                            writer.flush();
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }


            }
        }
        return false;
    }


    /**
     * 获取包名
     */
    private String getPackageName(VariableElement variableElement)
    {
        TypeElement typeElement=(TypeElement) variableElement.getEnclosingElement();
        PackageElement packageElement=processingEnv.getElementUtils().getPackageOf(typeElement);
        return packageElement.getQualifiedName().toString();
    }
}
