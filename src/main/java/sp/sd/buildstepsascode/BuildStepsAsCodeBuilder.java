package sp.sd.buildstepsascode;

import hudson.*;
import hudson.model.*;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.json.JSONArray;
import org.kohsuke.stapler.*;

import java.io.IOException;
import java.util.*;

import org.jenkinsci.plugins.structs.describable.DescribableModel;
import org.kohsuke.stapler.bind.JavaScriptMethod;

public class BuildStepsAsCodeBuilder extends Builder implements SimpleBuildStep {

    private final Builder buildStep;
    private final String buildContent;

    @DataBoundConstructor
    public BuildStepsAsCodeBuilder(Builder buildStep, String buildContent) {
        this.buildStep = buildStep;
        this.buildContent = buildContent;
    }

    public Builder getBuildStep() {
        return buildStep;
    }

    public String getBuildContent() {
        return buildContent;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        try {
            JSONArray buildSteps = new JSONArray(this.buildContent);
            Iterator itBuildSteps = buildSteps.iterator();
            while (itBuildSteps.hasNext()) {
                org.json.JSONObject jsonBuildStep = (org.json.JSONObject) itBuildSteps.next();

                Descriptor descriptor = getDescriptorFromName(jsonBuildStep.getString("stepClass"));
                DescribableModel describableModel = new DescribableModel<>(descriptor.clazz);
                Iterator itBuildStepDetails = jsonBuildStep.getJSONArray("stepDetails").iterator();
                while (itBuildStepDetails.hasNext()) {
                    org.json.JSONObject buildStep = (org.json.JSONObject) itBuildStepDetails.next();
                    Builder builder = (Builder) describableModel.instantiate(Util.jsonToMap(buildStep));
                    if (!builder.perform((AbstractBuild<?, ?>) build, launcher, (BuildListener) listener)) {
                        throw new AbortException(descriptor.getDisplayName() + " failed");
                    }
                }
            }
        } catch (Exception e) {
            throw new AbortException(e.getMessage());
        }
    }

    Descriptor getDescriptorFromName(String className) {
        for (hudson.model.Descriptor<Builder> descriptor : Builder.all()) {
            if (!(descriptor instanceof BuildStepDescriptor)) {
                continue;
            }
            if (descriptor.getKlass().toString().contains(className)) {
                return descriptor;
            }
        }
        return null;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    private static Random generator = new Random();

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            if (aClass == FreeStyleProject.class) {
                return true;
            }
            return false;
        }

        @JavaScriptMethod
        public String generateGroovy(String json, String key) throws Exception {
            try {
                StaplerRequest req = Stapler.getCurrentRequest();
                Jenkins j = Jenkins.getActiveInstance();
                net.sf.json.JSONObject jobJsonData = net.sf.json.JSONObject.fromObject(json);
                String builder = jobJsonData.getString("builder");
                String actualBuilder = "";
                if (builder.startsWith("{")) {
                    net.sf.json.JSONObject builderJsonData = net.sf.json.JSONObject.fromObject(builder);
                    actualBuilder = builderJsonData.getString("buildStep");
                } else if (builder.startsWith("[")) {
                    net.sf.json.JSONArray buildersListJsonData = net.sf.json.JSONArray.fromObject(builder);
                    for (Object builderObject : buildersListJsonData) {
                        net.sf.json.JSONObject builderJsonData = (net.sf.json.JSONObject) builderObject;
                        if (builderJsonData.getString("stapler-class").equals("sp.sd.buildstepsascode.BuildStepsAsCodeBuilder")) {
                            if (builderJsonData.getString("generatedKey").equals(key)) {
                                actualBuilder = builderJsonData.getString("buildStep");
                            }
                        }
                    }
                }
                net.sf.json.JSONObject actualBuilderObject = net.sf.json.JSONObject.fromObject(actualBuilder);
                Class<?> c = j.getPluginManager().uberClassLoader.loadClass(actualBuilderObject.getString("stapler-class"));
                Descriptor descriptor = (Descriptor) j.getDescriptor(c.asSubclass(Builder.class));
                DescribableModel describableModel = new DescribableModel<>(descriptor.clazz);
                Object o = descriptor.newInstance(req, actualBuilderObject);
                try {
                    return "[{\"stepClass\":\"" + actualBuilderObject.getString("stapler-class") + "\", \"stepDetails\":[" + Util.displayJSONMAP(describableModel.uninstantiate(o)) + "]}]";
                } catch (Exception e) {
                    return "[{\"stepClass\":\"" + actualBuilderObject.getString("stapler-class") + "\", \"stepDetails\":[" + Util.removeStaplerClass(actualBuilderObject) + "]}]";
                }
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        public String getDisplayName() {
            return "Build Steps as Code";
        }

        public ListBoxModel doFillBuildStepItems() {
            ListBoxModel items = new ListBoxModel();
            for (Descriptor descriptor : getApplicableDescriptors()) {
                items.add(descriptor.getDisplayName(), descriptor.getKlass().clazz.toString());
            }
            return items;
        }


        public Collection<? extends Descriptor<?>> getApplicableDescriptors() {
            List<Descriptor<?>> r = new ArrayList<Descriptor<?>>();
            populate(r, Builder.class);
            return r;
        }

        private <T extends Describable<T>, D extends Descriptor<T>> void populate(List<Descriptor<?>> r, Class<T> c) {
            Jenkins j = Jenkins.getInstance();
            if (j == null) {
                return;
            }
            for (Descriptor<?> d : j.getDescriptorList(c)) {
                if (!d.getKlass().equals(this.getKlass())) {
                    r.add(d);
                }
            }
        }

        private String generatedKey;

        public String getGeneratedKey() {
            if (generatedKey == null) {
                return Integer.toString(generator.nextInt(32000));
            } else {
                return generatedKey;
            }
        }
    }
}

