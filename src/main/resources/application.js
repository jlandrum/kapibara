function init() {
    let vm = new Vue({
        el: '#app',
        data: {
            api: null,
            selectedUrl: null,
            selectedHost: null,
            expandedAreas: {},
            responses: {},
            log: ""
        },
        methods: {
            httpMethods(methods) {
                let val = {};
                ["get","put","post","delete","options","head","patch","trace"]
                    .filter(method=>methods[method]!=null).forEach(it=>val[it] = methods[it]);
                return val
            },
            toggleView(name) {
                Vue.set(vm.expandedAreas, name, !(vm.expandedAreas[name]||false))
            },
            toggleActivated(event) {
                let element = event.target;
                element.classList.toggle("active")
            },
            executeApi(id, path, method) {
                if (typeof JB !== "undefined") {
                    var data = JB.executeApiCall(vm.selectedUrl,path,method);
                    vm.log = JSON.parse(data);
                    Vue.set(vm.responses, id, {"code": data.code||0, "message": data.message||"", "body": data.body||""});
                } else {
                    Vue.set(vm.responses, id, {"code": 3, "message": "test", "body": "Hello"});
                }
            }
        }
    });

    window.updateContent = function(obj) {
        vm.api = obj
    }
}



