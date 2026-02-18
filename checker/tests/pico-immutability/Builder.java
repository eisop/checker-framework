import org.checkerframework.checker.pico.qual.Mutable;

// Example taken from https://webagam.com/pages/2020/05/27/immutable-objects-with-builder-pattern/
// TODO this seems to be a good exmaple for fix the behavior for the interaction between PICO and
// initialization checker
public class Builder {
    public static class NoBuilderEmployee {
        private String id;
        private String name;
        private String department;
        private String organization;
        private String email;

        public NoBuilderEmployee(
                String id, String name, String department, String organization, String email) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.organization = organization;
            this.email = email;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDepartment() {
            return department;
        }

        public String getOrganization() {
            return organization;
        }

        public String getEmail() {
            return email;
        }

        public static void main(String args[]) {
            String id = "xyz123";
            String name = "user1";
            String department = "HR";
            String organization = "webagam Comp";
            String email = "user1@webagam.com";

            NoBuilderEmployee emp =
                    new NoBuilderEmployee(id, name, organization, department, email);
        }
    }

    public static class InnerClassBuilderEmployee {
        private String id;
        private String name;
        private String department;
        private String organization;
        private String email;

        private InnerClassBuilderEmployee(
                String id, String name, String department, String organization, String email) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.organization = organization;
            this.email = email;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDepartment() {
            return department;
        }

        public String getOrganization() {
            return organization;
        }

        public String getEmail() {
            return email;
        }

        public static @Mutable class InnerClassBuilder {

            private String id;
            private String name;
            private String department;
            private String organization;
            private String email;

            public InnerClassBuilder withId(String id) {
                this.id = id;
                return this;
            }

            public InnerClassBuilder withName(String name) {
                this.name = name;
                return this;
            }

            public InnerClassBuilder withDepartment(String department) {
                this.department = department;
                return this;
            }

            public InnerClassBuilder withOrganization(String organization) {
                this.organization = organization;
                return this;
            }

            public InnerClassBuilder withEmail(String email) {
                this.email = email;
                return this;
            }

            public InnerClassBuilderEmployee build() {
                return new InnerClassBuilderEmployee(id, name, department, organization, email);
            }
        }

        public static void main(String args[]) {
            String id = "xyz123";
            String name = "user1";
            String department = "HR";
            String organization = "webagam Comp";
            String email = "user1@webagam.com";
            InnerClassBuilderEmployee emp =
                    new InnerClassBuilderEmployee.InnerClassBuilder()
                            .withId(id)
                            .withName(name)
                            .withDepartment(department)
                            .withOrganization(organization)
                            .withEmail(email)
                            .build();
        }
    }

    // This kind of builder pattern is not supported and will require the help of uniqueness
    // property
    public static class ModernBuilderEmployee {
        private String id;
        private String name;
        private String department;
        private String organization;
        private String email;

        private void setId(String id) {
            // :: error: (illegal.field.write)
            this.id = id;
        }

        private void setName(String name) {
            // :: error: (illegal.field.write)
            this.name = name;
        }

        private void setDepartment(String department) {
            // :: error: (illegal.field.write)
            this.department = department;
        }

        private void setOrganization(String organization) {
            // :: error: (illegal.field.write)
            this.organization = organization;
        }

        private void setEmail(String email) {
            // :: error: (illegal.field.write)
            this.email = email;
        }

        // :: error: (initialization.fields.uninitialized)
        private ModernBuilderEmployee() {}

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDepartment() {
            return department;
        }

        public String getOrganization() {
            return organization;
        }

        public String getEmail() {
            return email;
        }

        public static @Mutable class ModernBuilder {
            ModernBuilderEmployee emp = new ModernBuilderEmployee();

            public ModernBuilder withId(String id) {
                emp.setId(id);
                return this;
            }

            public ModernBuilder withName(String name) {
                emp.setName(name);
                return this;
            }

            public ModernBuilder withDepartment(String department) {
                emp.setDepartment(department);
                return this;
            }

            public ModernBuilder withOrganization(String organization) {
                emp.setOrganization(organization);
                return this;
            }

            public ModernBuilder withEmail(String email) {
                emp.setEmail(email);
                return this;
            }

            public ModernBuilderEmployee build() {
                return emp;
            }
        }

        public static void main(String args[]) {
            String id = "xyz123";
            String name = "user1";
            String department = "HR";
            String organization = "webagam Comp";
            String email = "user1@webagam.com";
            ModernBuilderEmployee.ModernBuilder builder = new ModernBuilderEmployee.ModernBuilder();
            ModernBuilderEmployee emp =
                    builder.withId(id)
                            .withName(name)
                            .withDepartment(department)
                            .withOrganization(organization)
                            .withEmail(email)
                            .build();
        }
    }
}
