function init() {
    let vm = new Vue({
        el: '#app',
        data: {
            api: null,
            selectedUrl: null,
            selectedHost: null,
            expandedAreas: {},
            responses: {},
            values: {},
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
            executeApi(id, path, method, values) {
                if (typeof JB !== "undefined") {
                    var data = JB.executeApiCall(vm.selectedUrl,path,method, values);
                    Vue.set(vm.responses, id, {"code": data.code()||0, "message": data.message()||"", "body": data.body()||""});
                } else {
                    Vue.set(vm.responses, id, {"code": 3, "message": "test", "body": "Hello"});
                }
            }
        }
    });

    window.updateContent = function(obj) {
        vm.api = obj;
        var valueMap = obj.paths;
        var outputValueMap = {};
        Object.keys(obj.paths).forEach(path=>{
                outputValueMap[path] = {};
                Object.keys(vm.httpMethods(obj.paths[path])).forEach(method => {
                    outputValueMap[path][method] = {};
                    outputValueMap[path][method]['body'] = "";
                    outputValueMap[path][method]['header'] = {};
                    outputValueMap[path][method]['query'] = {};
                    outputValueMap[path][method]['path'] = {};
                });
            }
        );
        vm.values = outputValueMap
    }
}



